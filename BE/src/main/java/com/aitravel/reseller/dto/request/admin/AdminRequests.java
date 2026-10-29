package com.aitravel.reseller.dto.request.admin;

import jakarta.validation.constraints.*;

public final class AdminRequests {
    private AdminRequests() {}

    public record StatusChange(
            @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED") String status,
            @Size(max = 1000) String reason,
            @NotNull @PositiveOrZero Long version) {}

    public record SettingsChange(
            @NotBlank @Pattern(regexp = "SUBDOMAIN_PREVIEW") String routeMode,
            @NotBlank @Size(max = 253)
            @Pattern(regexp = "(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}") String baseDomain,
            @NotNull @Size(max = 10000) String captionTemplate,
            @NotNull @Size(max = 10000) String videoTemplate,
            @NotNull @PositiveOrZero Long version) {}
}
