package com.aitravel.reseller;

import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.reseller.ResellerApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResellerApplicationIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResellerApplicationService service;
    @Autowired private MockMvc mvc;

    private final UUID merchantUserId = UUID.randomUUID();
    private final UUID resellerUserId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final UUID resellerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void createFixture() {
        for (UUID id : List.of(merchantUserId, resellerUserId)) {
            jdbc.update("INSERT INTO travel_v2.users (id,email,password_hash,full_name,status) VALUES (?,?,?,?,?)",
                    id, id + "@example.test", "hash", "Test user", "ACTIVE");
        }
        jdbc.update("INSERT INTO travel_v2.merchants (id,user_id,code,name,contact_email,subdomain,status) VALUES (?,?,?,?,?,?,?)",
                merchantId, merchantUserId, "M" + merchantId, "Test merchant", "merchant@example.test",
                "m" + merchantId, "ACTIVE");
        jdbc.update("INSERT INTO travel_v2.reseller_profiles (id,user_id,status) VALUES (?,?,?)",
                resellerId, resellerUserId, "ACTIVE");
        jdbc.update("INSERT INTO travel_v2.products (id,merchant_id,title,slug,price,availability_status,official_checkout_url) VALUES (?,?,?,?,?,?,?)",
                productId, merchantId, "Test product", "p" + productId, new BigDecimal("100.00"),
                "AVAILABLE", "https://example.test/checkout");
        jdbc.update("INSERT INTO travel_v2.campaigns (id,product_id,title,commission_type,commission_rate,status) VALUES (?,?,?,?,?,?)",
                campaignId, productId, "Test campaign", "PERCENTAGE", new BigDecimal("12.50"), "ACTIVE");
        authenticateReseller();
    }

    @AfterEach
    void removeFixture() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM travel_v2.campaign_applications WHERE campaign_id = ?", campaignId);
        jdbc.update("DELETE FROM travel_v2.campaigns WHERE id = ?", campaignId);
        jdbc.update("DELETE FROM travel_v2.products WHERE id = ?", productId);
        jdbc.update("DELETE FROM travel_v2.reseller_profiles WHERE id = ?", resellerId);
        jdbc.update("DELETE FROM travel_v2.merchants WHERE id = ?", merchantId);
        jdbc.update("DELETE FROM travel_v2.users WHERE id IN (?,?)", merchantUserId, resellerUserId);
    }

    private void authenticateReseller() {
        var user = new CustomUserDetails(resellerUserId, "reseller@example.test",
                List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void actualSchemaRequiresBothSnapshotsAndUniqueApplication() {
        var columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'travel_v2'
                  AND table_name = 'campaign_applications'
                  AND column_name IN ('commission_type_snapshot', 'commission_rate_snapshot')
                  AND is_nullable = 'NO'
                """, String.class);
        assertEquals(2, columns.size());
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE connamespace = 'travel_v2'::regnamespace
                  AND conname = 'uq_campaign_application'
                """, Integer.class));
    }

    @Test
    void firstApplyStoresCampaignSnapshotAndSecondApplyReturns409() {
        var result = service.apply(campaignId);
        assertEquals("PENDING", result.status());
        var snapshot = jdbc.queryForMap("""
                SELECT commission_type_snapshot, commission_rate_snapshot
                FROM travel_v2.campaign_applications WHERE id = ?
                """, result.id());
        assertEquals("PERCENTAGE", snapshot.get("commission_type_snapshot"));
        assertEquals(0, new BigDecimal("12.50")
                .compareTo((BigDecimal) snapshot.get("commission_rate_snapshot")));

        jdbc.update("UPDATE travel_v2.campaigns SET commission_rate = ? WHERE id = ?",
                new BigDecimal("15.00"), campaignId);
        var unchanged = jdbc.queryForObject("""
                SELECT commission_rate_snapshot FROM travel_v2.campaign_applications WHERE id = ?
                """, BigDecimal.class, result.id());
        assertEquals(0, new BigDecimal("12.50").compareTo(unchanged));

        var duplicate = assertThrows(ResponseStatusException.class, () -> service.apply(campaignId));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(1, applicationCount());
    }

    @Test
    void simultaneousApplyCreatesExactlyOneApplication() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Integer>) () -> {
                authenticateReseller();
                try {
                    start.await();
                    service.apply(campaignId);
                    return 201;
                } catch (ResponseStatusException e) {
                    return e.getStatusCode().value();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            start.countDown();
            assertEquals(List.of(201, 409), List.of(first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)).stream().sorted().toList());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, applicationCount());
    }

    @Test
    void missingCampaignReturns404() {
        var error = assertThrows(ResponseStatusException.class, () -> service.apply(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void httpApplyReturns201Then409AndMissingCampaignReturns404() throws Exception {
        var user = new CustomUserDetails(resellerUserId, "reseller@example.test",
                List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String path = "/api/v1/reseller/campaigns/" + campaignId + "/applications";
        mvc.perform(post(path).with(authentication(auth))).andExpect(status().isCreated());
        mvc.perform(post(path).with(authentication(auth))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/reseller/campaigns/" + UUID.randomUUID() + "/applications")
                .with(authentication(auth))).andExpect(status().isNotFound());
    }

    @Test
    void httpApplyRejectsNonResellerWith403() throws Exception {
        var user = new CustomUserDetails(resellerUserId, "merchant@example.test",
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        mvc.perform(post("/api/v1/reseller/campaigns/" + campaignId + "/applications")
                .with(authentication(auth))).andExpect(status().isForbidden());
        assertEquals(0, applicationCount());
    }

    private int applicationCount() {
        return jdbc.queryForObject("SELECT count(*) FROM travel_v2.campaign_applications WHERE campaign_id = ?",
                Integer.class, campaignId);
    }
}
