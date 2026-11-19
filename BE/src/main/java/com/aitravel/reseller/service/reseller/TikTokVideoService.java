package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.TikTokVideoItemResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokVideoListResponse;
import com.aitravel.reseller.entity.ResellerProfile;
import com.aitravel.reseller.entity.SocialAccountOAuthCredential;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.SocialAccountOAuthCredentialRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.SocialTokenCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TikTokVideoService {
    private static final String PROVIDER = "TIKTOK";
    private static final String REQUIRED_SCOPE = "video.list";
    private static final String VIDEO_FIELDS = "id,title,video_description,create_time,cover_image_url,share_url,embed_link,view_count,like_count,comment_count,share_count";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10)).build();

    private final ResellerProfileRepository profiles;
    private final SocialAccountOAuthCredentialRepository credentials;
    private final SocialTokenCipher cipher;
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public TikTokVideoListResponse listVideos(UUID socialAccountId, int maxCount, Long cursor) {
        if (maxCount < 1 || maxCount > 20) {
            throw new IllegalArgumentException("maxCount must be between 1 and 20");
        }

        ResellerProfile reseller = currentActiveReseller();
        SocialAccountOAuthCredential credential = credentials
                .findOwnedCredential(socialAccountId, reseller.getId(), PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException("Linked TikTok account was not found"));

        if (!"ACTIVE".equals(credential.getSocialAccount().getStatus())) {
            throw new IllegalStateException("The linked TikTok account is not active");
        }
        if (!hasScope(credential.getScopes(), REQUIRED_SCOPE)) {
            throw new IllegalStateException("Reconnect the TikTok account and grant the video.list permission");
        }
        if (credential.getTokenExpiresAt() == null || !credential.getTokenExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("The TikTok access token has expired; reconnect the account");
        }

        try {
            String accessToken = cipher.decrypt(credential.getAccessTokenCiphertext());
            URI uri = UriComponentsBuilder.fromUriString("https://open.tiktokapis.com/v2/video/list/")
                    .queryParam("fields", VIDEO_FIELDS).build().encode().toUri();
            var requestBody = mapper.createObjectNode().put("max_count", maxCount);
            if (cursor != null) requestBody.put("cursor", cursor);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(java.time.Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = mapper.readTree(response.body());
            JsonNode error = body.path("error");
            if (response.statusCode() < 200 || response.statusCode() >= 300
                    || (error.isObject() && !"ok".equals(error.path("code").asText("ok")))) {
                throw new IllegalStateException("TikTok could not return videos for this account");
            }

            JsonNode data = body.path("data");
            List<TikTokVideoItemResponse> videos = new ArrayList<>();
            for (JsonNode video : data.path("videos")) {
                long createdAt = video.path("create_time").asLong(0);
                videos.add(TikTokVideoItemResponse.builder()
                        .id(text(video, "id"))
                        .title(text(video, "title"))
                        .description(text(video, "video_description"))
                        .createTime(createdAt > 0 ? Instant.ofEpochSecond(createdAt) : null)
                        .coverImageUrl(text(video, "cover_image_url"))
                        .shareUrl(text(video, "share_url"))
                        .embedLink(text(video, "embed_link"))
                        .viewCount(number(video, "view_count"))
                        .likeCount(number(video, "like_count"))
                        .commentCount(number(video, "comment_count"))
                        .shareCount(number(video, "share_count"))
                        .build());
            }

            return TikTokVideoListResponse.builder()
                    .socialAccountId(socialAccountId)
                    .videos(videos)
                    .cursor(data.hasNonNull("cursor") ? data.path("cursor").asLong() : null)
                    .hasMore(data.path("has_more").asBoolean(false))
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The TikTok video request was interrupted");
        } catch (IOException e) {
            throw new IllegalStateException("Could not communicate with the TikTok video API");
        }
    }

    /**
     * TikTok's video.list API lists videos for the authorized account; it does not
     * provide a direct lookup-by-video-id endpoint. Walk its cursor pages until the
     * requested video is found or TikTok reports that there are no more pages.
     */
    @Transactional(readOnly = true)
    public TikTokVideoItemResponse findVideo(UUID socialAccountId, String videoId) {
        if (videoId == null || !videoId.matches("[0-9]{1,32}")) {
            throw new IllegalArgumentException("videoId must contain only digits");
        }

        Long cursor = null;
        Set<Long> visitedCursors = new HashSet<>();
        do {
            TikTokVideoListResponse page = listVideos(socialAccountId, 20, cursor);
            for (TikTokVideoItemResponse video : page.getVideos()) {
                if (videoId.equals(video.getId())) return video;
            }
            if (!page.isHasMore() || page.getCursor() == null || !visitedCursors.add(page.getCursor())) {
                break;
            }
            cursor = page.getCursor();
        } while (true);

        throw new ResourceNotFoundException("TikTok video was not found in the linked account's accessible videos");
    }

    @Transactional(readOnly = true)
    public TikTokVideoListResponse findVideoPage(UUID socialAccountId, String videoId) {
        TikTokVideoItemResponse video = findVideo(socialAccountId, videoId);
        return TikTokVideoListResponse.builder()
                .socialAccountId(socialAccountId)
                .videos(List.of(video))
                .cursor(null)
                .hasMore(false)
                .build();
    }

    @Transactional(readOnly = true)
    public TikTokVideoListResponse listCurrentResellerVideos(int maxCount, Long cursor) {
        return listVideos(resolveSingleActiveAccountId(), maxCount, cursor);
    }

    @Transactional(readOnly = true)
    public TikTokVideoListResponse findCurrentResellerVideoPage(String videoId) {
        return findVideoPage(resolveSingleActiveAccountId(), videoId);
    }

    private UUID resolveSingleActiveAccountId() {
        ResellerProfile reseller = currentActiveReseller();
        List<SocialAccountOAuthCredential> linkedAccounts = credentials
                .findActiveOwnedCredentials(reseller.getId(), PROVIDER);
        if (linkedAccounts.isEmpty()) {
            throw new ResourceNotFoundException("No active TikTok account is connected to this reseller");
        }
        if (linkedAccounts.size() > 1) {
            throw new IllegalStateException("More than one active TikTok account is connected; specify socialAccountId");
        }
        return linkedAccounts.get(0).getSocialAccount().getId();
    }

    private boolean hasScope(String scopes, String required) {
        if (scopes == null) return false;
        for (String scope : scopes.split("[,\\s]+")) {
            if (required.equals(scope.trim())) return true;
        }
        return false;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asLong();
    }

    private ResellerProfile currentActiveReseller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        boolean resellerRole = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RESELLER".equals(authority.getAuthority()));
        if (!user.isEnabled() || !resellerRole) throw new AccessDeniedException("Reseller role required");
        ResellerProfile profile = profiles.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Reseller profile required"));
        if (!"ACTIVE".equals(profile.getStatus())) throw new AccessDeniedException("Active reseller profile required");
        return profile;
    }
}
