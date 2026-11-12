package com.aitravel.reseller.dto.respone.reseller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TikTokOAuthUrlResponse {
    private String authorizationUrl;
    private long expiresInSeconds;
}
