package com.aitravel.reseller.dto.respone.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOtpVerifyResponse {
    private String onboardingToken;
    private String email;
    private String fullName;
    private String businessName;
    private String suggestedSubdomain;
}
