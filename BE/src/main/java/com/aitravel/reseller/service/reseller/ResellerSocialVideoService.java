package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.request.reseller.RegisterSocialVideoRequest;
import com.aitravel.reseller.dto.respone.reseller.SocialVideoResponse;
import com.aitravel.reseller.entity.CampaignApplication;
import com.aitravel.reseller.entity.ResellerCampaignPost;
import com.aitravel.reseller.entity.ResellerProfile;
import com.aitravel.reseller.entity.SocialAccount;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.CampaignApplicationRepository;
import com.aitravel.reseller.repository.ResellerCampaignPostRepository;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.SocialAccountRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResellerSocialVideoService {
    private static final Pattern TIKTOK_VIDEO_PATH = Pattern.compile("^/@[^/]+/video/(\\d+)(?:/.*)?/?$");
    private static final Pattern FACEBOOK_REEL_PATH = Pattern.compile("^/(?:[^/]+/)?(?:reel|videos)/(\\d+)(?:/.*)?/?$");

    private final ResellerCampaignPostRepository postRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final ResellerProfileRepository profileRepository;

    @Transactional
    public SocialVideoResponse register(RegisterSocialVideoRequest request) {
        UUID resellerId = currentResellerId();
        ParsedVideo parsed = parseVideoUrl(request.postUrl());

        SocialAccount socialAccount = socialAccountRepository
                .findByIdAndResellerId(request.socialAccountId(), resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked social account was not found"));

        if (!"ACTIVE".equals(socialAccount.getStatus())) {
            throw new IllegalStateException("The linked social account is not active");
        }
        if (!parsed.platform().equals(socialAccount.getPlatform())) {
            throw new IllegalArgumentException("The video platform does not match the selected social account");
        }

        CampaignApplication application = applicationRepository
                .findByIdAndResellerId(request.campaignApplicationId(), resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign application was not found"));
        if (!"APPROVED".equals(application.getStatus())) {
            throw new IllegalStateException("A video can only be added to an approved campaign application");
        }

        if (postRepository.findBySocialAccountIdAndPlatformAndExternalVideoId(
                socialAccount.getId(), parsed.platform(), parsed.videoId()).isPresent()) {
            throw new IllegalStateException("This video is already registered for the selected social account");
        }

        Instant now = Instant.now();
        ResellerCampaignPost post = ResellerCampaignPost.builder()
                .campaignApplication(application)
                .socialAccount(socialAccount)
                .postUrl(parsed.normalizedUrl())
                .platform(parsed.platform())
                .externalVideoId(parsed.videoId())
                .viewsCount(0L)
                .likesCount(0L)
                .metricsStatus("PENDING")
                .status("PUBLISHED")
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(postRepository.save(post));
    }

    private ParsedVideo parseVideoUrl(String rawUrl) {
        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("The video URL is invalid");
        }

        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Only valid HTTPS video URLs are accepted");
        }
        host = host.toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "/" : uri.getPath();

        if (isHost(host, "tiktok.com")) {
            Matcher matcher = TIKTOK_VIDEO_PATH.matcher(path);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("TikTok URL must use the /@username/video/{videoId} format");
            }
            return new ParsedVideo("TIKTOK", matcher.group(1), uri.toString());
        }

        if (isHost(host, "facebook.com")) {
            String videoId = queryParameter(uri.getRawQuery(), "v");
            if (videoId == null) {
                Matcher matcher = FACEBOOK_REEL_PATH.matcher(path);
                if (matcher.matches()) videoId = matcher.group(1);
            }
            if (videoId == null || !videoId.matches("\\d+")) {
                throw new IllegalArgumentException("Facebook URL must contain a supported video or reel ID");
            }
            return new ParsedVideo("FACEBOOK", videoId, uri.toString());
        }

        throw new IllegalArgumentException("Only TikTok and Facebook video URLs are supported");
    }

    private boolean isHost(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private String queryParameter(String rawQuery, String requestedName) {
        if (rawQuery == null || rawQuery.isBlank()) return null;
        for (String part : rawQuery.split("&")) {
            String[] keyValue = part.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            if (requestedName.equals(key) && keyValue.length == 2) {
                return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private UUID currentResellerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        boolean hasResellerRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RESELLER".equals(authority.getAuthority()));
        if (!user.isEnabled() || !hasResellerRole) {
            throw new AccessDeniedException("Reseller role required");
        }

        ResellerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Reseller profile required"));
        if (!"ACTIVE".equals(profile.getStatus())) {
            throw new AccessDeniedException("Active reseller profile required");
        }
        return profile.getId();
    }

    private SocialVideoResponse toResponse(ResellerCampaignPost post) {
        return new SocialVideoResponse(
                post.getId(),
                post.getPlatform(),
                post.getExternalVideoId(),
                post.getPostUrl(),
                post.getViewsCount(),
                post.getLikesCount(),
                post.getMetricsStatus(),
                post.getMetricsFetchedAt(),
                post.getCreatedAt());
    }

    private record ParsedVideo(String platform, String videoId, String normalizedUrl) {}
}
