package com.aitravel.reseller.dto.respone.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubdomainCheckResponse {
    private String subdomain;
    private boolean available;
    private String message;
}
