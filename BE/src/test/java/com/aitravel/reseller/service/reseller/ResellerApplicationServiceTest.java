package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.ApplicationResponse;
import com.aitravel.reseller.entity.ResellerProfile;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.reseller.ResellerApplicationRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResellerApplicationServiceTest {
    private final ResellerApplicationRepository repository = mock(ResellerApplicationRepository.class);
    private final ResellerProfileRepository profiles = mock(ResellerProfileRepository.class);
    private final ResellerApplicationService service = new ResellerApplicationService(repository, profiles);
    private final UUID campaignId = UUID.randomUUID();
    private final UUID resellerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void resellerAuthentication() {
        authenticate("ROLE_RESELLER");
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(
                ResellerProfile.builder().id(resellerId).status("ACTIVE").build()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String role) {
        var authority = new SimpleGrantedAuthority(role);
        var user = new CustomUserDetails(userId, "reseller@example.test", List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void missingCampaignReturns404BeforeInsert() {
        var error = assertThrows(ResponseStatusException.class, () -> service.apply(campaignId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).create(any(), any());
    }

    @Test
    void nonResellerReturns403BeforeDatabaseChecks() {
        authenticate("ROLE_MERCHANT");
        assertThrows(AccessDeniedException.class, () -> service.apply(campaignId));
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateReturns409BeforeInsert() {
        when(repository.campaignExists(campaignId)).thenReturn(true);
        when(repository.isCampaignEligible(campaignId)).thenReturn(true);
        when(repository.applicationExists(campaignId, resellerId)).thenReturn(true);

        var error = assertThrows(ResponseStatusException.class, () -> service.apply(campaignId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).create(any(), any());
    }

    @Test
    void firstApplicationSucceeds() {
        var response = new ApplicationResponse(UUID.randomUUID(), campaignId, "PENDING", Instant.now());
        when(repository.campaignExists(campaignId)).thenReturn(true);
        when(repository.isCampaignEligible(campaignId)).thenReturn(true);
        when(repository.create(campaignId, resellerId)).thenReturn(Optional.of(response));

        assertSame(response, service.apply(campaignId));
        verify(repository).applicationExists(campaignId, resellerId);
    }

    @Test
    void concurrentConflictReturns409() {
        when(repository.campaignExists(campaignId)).thenReturn(true);
        when(repository.isCampaignEligible(campaignId)).thenReturn(true);
        when(repository.create(campaignId, resellerId)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class, () -> service.apply(campaignId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void uniqueConstraintViolationReturns409() {
        when(repository.campaignExists(campaignId)).thenReturn(true);
        when(repository.isCampaignEligible(campaignId)).thenReturn(true);
        when(repository.create(campaignId, resellerId)).thenThrow(new DuplicateKeyException("duplicate"));

        var error = assertThrows(ResponseStatusException.class, () -> service.apply(campaignId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
