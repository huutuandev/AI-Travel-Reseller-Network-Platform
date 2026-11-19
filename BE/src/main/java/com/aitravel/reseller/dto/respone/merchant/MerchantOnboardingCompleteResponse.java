package com.aitravel.reseller.dto.respone.merchant;

import com.aitravel.reseller.dto.respone.AuthResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOnboardingCompleteResponse {
    private AuthResponse auth;
    private MerchantProfileResponse merchant;
}
