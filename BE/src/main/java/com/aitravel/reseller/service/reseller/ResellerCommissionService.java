package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.CommissionResponse;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.reseller.ResellerCommissionRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerCommissionService {

    private final ResellerCommissionRepository repository;
    private final ResellerProfileRepository profiles;

    public List<CommissionResponse> getMyCommissions() {

        UUID resellerId = currentResellerId();

        return repository.findAllByResellerId(resellerId);
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
            throw new AccessDeniedException(
                    "Active reseller profile required");
        }

        return profile.getId();
    }
}