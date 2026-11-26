package com.aitravel.reseller.dto.respone.reseller;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TikTokConnectedAccountResponse {
    private UUID socialAccountId;
    private String platform;
    private String username;
    private String displayName;
    private String status;
    private Instant tokenExpiresAt;
    private String[] scopes;
}
