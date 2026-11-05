package com.aitravel.reseller.service.admin;

import com.aitravel.reseller.dto.request.admin.AdminRequests;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.admin.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final AdminRepository db;
    private static final String TENANT_COLUMNS = """
        m.id, m.code, m.name AS "businessName", m.description, m.subdomain,
        m.custom_domain AS "customDomain", m.status, m.created_at AS "createdAt", m.version
        """;

    public record PageResult(List<Map<String, Object>> content, long totalElements,
                             long totalPages, int number, int size) {}

    private void pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0; size must be 1..100");
    }

    public PageResult tenants(int page, int size, String search, String status) {
        pagination(page, size);
        if (search.length() > 200) throw new IllegalArgumentException("Search is too long");
        if (!status.isEmpty() && !Set.of("ACTIVE", "PENDING", "SUSPENDED").contains(status))
            throw new IllegalArgumentException("Invalid tenant status");
        String where = """
            FROM travel_v2.merchants m
            WHERE (:status = '' OR m.status = :status)
            AND (:search = '' OR strpos(lower(m.name), lower(:search)) > 0
                OR strpos(lower(m.code), lower(:search)) > 0
                OR strpos(lower(m.subdomain), lower(:search)) > 0
                OR CAST(m.id AS text) = :search)
            """;
        Map<String, Object> p = new HashMap<>(Map.of("status", status, "search", search.trim(),
                "size", size, "offset", (long) page * size));
        long total = db.count("SELECT count(*) " + where, p);
        return new PageResult(db.rows("SELECT " + TENANT_COLUMNS + where +
                " ORDER BY m.created_at DESC, m.id LIMIT :size OFFSET :offset", p), total,
                (total + size - 1) / size, page, size);
    }

    private Map<String, Object> one(String sql, Map<String, ?> p) {
        var rows = db.rows(sql, p);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Resource not found");
        return new LinkedHashMap<>(rows.get(0));
    }

    public Map<String, Object> tenant(UUID id) {
        var p = Map.of("id", id);
        var result = one("SELECT " + TENANT_COLUMNS + " FROM travel_v2.merchants m WHERE m.id=:id", p);
        result.put("contact", one("""
            SELECT u.full_name AS name, u.email AS "ownerEmail", m.contact_email AS email,
                   m.phone, m.address FROM travel_v2.merchants m
            JOIN travel_v2.users u ON u.id=m.user_id WHERE m.id=:id
            """, p));
        var branding = db.rows("""
            SELECT logo_url AS "logoUrl", primary_color AS "primaryColor", secondary_color AS "secondaryColor"
            FROM travel_v2.merchant_branding WHERE merchant_id=:id
            """, p);
        result.put("branding", branding.isEmpty() ? null : branding.get(0));
        var stats = one("""
            SELECT
              (SELECT count(*) FROM travel_v2.campaigns c JOIN travel_v2.products p ON p.id=c.product_id
               JOIN travel_v2.merchants m ON m.id=p.merchant_id
               WHERE m.id=:id AND m.status='ACTIVE' AND c.status='ACTIVE'
               AND (c.start_date IS NULL OR c.start_date<=now())
               AND (c.end_date IS NULL OR c.end_date>now())) AS "activeCampaigns",
              (SELECT count(DISTINCT a.reseller_id) FROM travel_v2.campaign_applications a
               JOIN travel_v2.campaigns c ON c.id=a.campaign_id
               JOIN travel_v2.products p ON p.id=c.product_id
               WHERE p.merchant_id=:id AND a.status='APPROVED') AS "totalResellers",
              (SELECT count(*) FROM travel_v2.bookings b WHERE b.merchant_id=:id
               AND b.tracking_link_id IS NOT NULL AND b.booking_status='CONFIRMED') AS "attributedBookings"
            """, p);
        stats.put("clicks30Days", null);
        stats.put("clicksAvailability", "NOT_COLLECTED");
        stats.put("bookingsPeriod", "ALL_TIME");
        result.put("stats", stats);
        return result;
    }

    @Transactional
    public Map<String, Object> changeStatus(UUID id, AdminRequests.StatusChange request, UUID actor) {
        if (request == null || !("ACTIVE".equals(request.status()) || "SUSPENDED".equals(request.status())))
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        if (request.version() == null || request.version() < 0)
            throw new IllegalArgumentException("A non-negative version is required");
        if (request.reason() != null && request.reason().length() > 1000)
            throw new IllegalArgumentException("Reason must not exceed 1000 characters");
        if ("SUSPENDED".equals(request.status()) && (request.reason() == null || request.reason().isBlank()))
            throw new IllegalArgumentException("Suspension reason is required");
        var p = new HashMap<String, Object>();
        p.put("id", id);
        var current = one("SELECT status, version FROM travel_v2.merchants WHERE id=:id FOR UPDATE", p);
        String old = (String) current.get("status");
        if (((Number) current.get("version")).longValue() != request.version())
            throw new IllegalStateException("Tenant changed; reload before updating");
        if (!("ACTIVE".equals(request.status()) && "SUSPENDED".equals(old))
                && !("SUSPENDED".equals(request.status()) && "ACTIVE".equals(old)))
            throw new IllegalStateException("Invalid tenant status transition");
        p.put("status", request.status());
        p.put("version", request.version());
        int changed = db.update("UPDATE travel_v2.merchants SET status=:status, version=version+1 WHERE id=:id AND version=:version", p);
        if (changed != 1)
            throw new IllegalStateException("Tenant changed; reload before updating");
        event(id, actor, "TENANT_STATUS_CHANGED", "TENANT", old, request.status(), request.reason());
        return tenant(id);
    }

    private void event(UUID merchant, UUID actor, String type, String area, String old, String next, String reason) {
        var p = new HashMap<String, Object>();
        p.put("merchant", merchant); p.put("actor", actor); p.put("type", type); p.put("area", area);
        p.put("old", old); p.put("next", next); p.put("reason", reason);
        db.update("""
            INSERT INTO travel_v2.platform_events(merchant_id,actor_user_id,event_type,area,result,previous_status,new_status,reason)
            VALUES (:merchant,:actor,:type,:area,'SUCCESS',:old,:next,:reason)
            """, p);
    }

    public Map<String, Object> settings() {
        return one("""
            SELECT route_mode AS "routeMode", base_domain AS "baseDomain", caption_template AS "captionTemplate",
            video_template AS "videoTemplate", version, updated_at AS "updatedAt", updated_by AS "updatedBy"
            FROM travel_v2.platform_settings WHERE id=1
            """, Map.of());
    }

    @Transactional
    public Map<String, Object> settings(AdminRequests.SettingsChange r, UUID actor) {
        int changed = db.update("""
            UPDATE travel_v2.platform_settings SET route_mode=:mode,base_domain=:domain,
            caption_template=:caption,video_template=:video,version=version+1,updated_by=:actor,updated_at=now()
            WHERE id=1 AND version=:version
            """, Map.of("mode", r.routeMode(), "domain", r.baseDomain(), "caption", r.captionTemplate(),
                "video", r.videoTemplate(), "actor", actor, "version", r.version()));
        if (changed != 1) throw new IllegalStateException("Settings changed; reload before updating");
        event(null, actor, "PREVIEW_SETTINGS_UPDATED", "PLATFORM", null, null, null);
        return settings();
    }

    public PageResult logs(UUID merchant, int page, int size, boolean tenantsOnly) {
        pagination(page, size);
        if (merchant != null) one("SELECT id FROM travel_v2.merchants WHERE id=:id", Map.of("id", merchant));
        var p = new HashMap<String, Object>(Map.of("size", size, "offset", (long) page * size));
        String where = " FROM travel_v2.platform_events e LEFT JOIN travel_v2.merchants m ON m.id=e.merchant_id WHERE true ";
        if (merchant != null) { where += " AND e.merchant_id=:merchant "; p.put("merchant", merchant); }
        if (tenantsOnly) where += " AND e.merchant_id IS NOT NULL ";
        long total = db.count("SELECT count(*) " + where, p);
        return new PageResult(db.rows("""
            SELECT e.id, e.merchant_id AS "tenantId", m.code AS "tenantCode", m.name AS "businessName",
            m.subdomain, e.actor_user_id AS "actorUserId", e.event_type AS "eventType", e.area, e.result,
            e.previous_status AS "previousStatus", e.new_status AS "newStatus", e.reason, e.created_at AS "createdAt"
            """ + where + " ORDER BY e.created_at DESC,e.id LIMIT :size OFFSET :offset", p), total,
                (total + size - 1) / size, page, size);
    }

    public Map<String, Object> overview(int days) {
        if (days < 1 || days > 366) throw new IllegalArgumentException("days must be 1..366");
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(days));
        Instant month = to.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withDayOfMonth(1).toLocalDate()
                .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        var p = Map.of("from", java.sql.Timestamp.from(from), "to", java.sql.Timestamp.from(to),
                "month", java.sql.Timestamp.from(month));
        var metrics = one("""
            SELECT count(*) AS "totalTenants",
            count(*) FILTER (WHERE created_at>=:month AND created_at<:to) AS "newTenantsThisMonth",
            count(*) FILTER (WHERE status='ACTIVE') AS "activeTenants",
            count(*) FILTER (WHERE status='PENDING') AS "pendingTenants",
            count(*) FILTER (WHERE status='SUSPENDED') AS "suspendedTenants"
            FROM travel_v2.merchants
            """, p);
        long total = ((Number) metrics.get("totalTenants")).longValue();
        long active = ((Number) metrics.get("activeTenants")).longValue();
        metrics.put("activePercentage", total == 0 ? 0 : Math.round(active * 1000.0 / total) / 10.0);
        var usage = one("""
            SELECT (SELECT count(*) FROM travel_v2.bookings WHERE tracking_link_id IS NOT NULL
                AND booking_status='CONFIRMED' AND created_at>=:from AND created_at<:to) AS "attributedBookings",
            (SELECT count(*) FROM travel_v2.campaigns c JOIN travel_v2.products p ON p.id=c.product_id
                JOIN travel_v2.merchants m ON m.id=p.merchant_id WHERE c.status='ACTIVE' AND m.status='ACTIVE'
                AND (c.start_date IS NULL OR c.start_date<=:to)
                AND (c.end_date IS NULL OR c.end_date>:to)) AS "runningCampaigns"
            """, p);
        usage.put("trackedClicks", null);
        usage.put("clicksAvailability", "NOT_COLLECTED");
        usage.put("runningCampaignsPeriod", "CURRENT");
        return Map.of("metrics", metrics, "usage", usage, "from", from, "to", to, "timezone", "Asia/Ho_Chi_Minh");
    }
}
