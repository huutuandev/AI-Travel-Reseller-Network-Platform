package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.ApplicationResponse;
import com.aitravel.reseller.dto.respone.reseller.RegisteredCampaignResponse;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.reseller.ResellerApplicationRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;

import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResellerApplicationService {

    private final ResellerApplicationRepository repository;
    private final ResellerProfileRepository profiles;

    @Transactional
    public ApplicationResponse apply(UUID campaignId) {

        if (campaignId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campaign ID is required");
        }

        UUID resellerId = currentResellerId();

        if (!repository.isCampaignEligible(campaignId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campaign is not available for application");
        }

        return repository.create(campaignId, resellerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "You have already applied to this campaign"));
    }

    private UUID currentResellerId() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()));

        if (!user.isEnabled() || !hasRole) {
            throw new AccessDeniedException("Reseller role required");
        }

        var profile = profiles.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Reseller profile required"));

        if (!"ACTIVE".equals(profile.getStatus())) {
            throw new AccessDeniedException("Active reseller profile required");
        }

        return profile.getId();
    }

    @Transactional(readOnly = true)
    public List<RegisteredCampaignResponse> getMyApplications() {

        UUID resellerId = currentResellerId();

        return repository.findAllByResellerId(resellerId);
    }
}