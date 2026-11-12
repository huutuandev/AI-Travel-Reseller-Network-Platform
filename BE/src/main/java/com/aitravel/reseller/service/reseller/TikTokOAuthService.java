package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.TikTokConnectedAccountResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokOAuthUrlResponse;
import com.aitravel.reseller.entity.ResellerProfile;
import com.aitravel.reseller.entity.SocialAccount;
import com.aitravel.reseller.entity.SocialAccountOAuthCredential;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.SocialAccountOAuthCredentialRepository;
import com.aitravel.reseller.repository.SocialAccountRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.RedisService;
import com.aitravel.reseller.service.SocialTokenCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TikTokOAuthService {
    private static final String PROVIDER = "TIKTOK";
    private static final String STATE_PREFIX = "tiktok:oauth:state:";
    private static final long STATE_TTL_SECONDS = 600;
    private static final String SCOPES = "user.info.basic,video.list";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();

    private final RedisService redis;
    private final ResellerProfileRepository profiles;
    private final SocialAccountRepository accounts;
    private final SocialAccountOAuthCredentialRepository credentials;
    private final SocialTokenCipher cipher;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.integrations.tiktok.client-key:}") private String clientKey;
    @Value("${app.integrations.tiktok.client-secret:}") private String clientSecret;
    @Value("${app.integrations.tiktok.redirect-uri:}") private String redirectUri;

    public TikTokOAuthUrlResponse createAuthorizationUrl() {
        ResellerProfile profile = currentActiveReseller();
        requireConfiguration();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        byte[] verifierBytes = new byte[32];
        RANDOM.nextBytes(verifierBytes);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
        String codeChallenge = createCodeChallenge(codeVerifier);
        redis.save(STATE_PREFIX + state, profile.getId() + ":" + codeVerifier, STATE_TTL_SECONDS, TimeUnit.SECONDS);
        String url = UriComponentsBuilder.fromUriString("https://www.tiktok.com/v2/auth/authorize/")
                .queryParam("client_key", clientKey)
                .queryParam("scope", SCOPES)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build().encode().toUriString();
        return new TikTokOAuthUrlResponse(url, STATE_TTL_SECONDS);
    }

    @Transactional
    public TikTokConnectedAccountResponse complete(String code, String state, String providerError) {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("OAuth state is required");
        String stateData = redis.getAndDelete(STATE_PREFIX + state);
        if (stateData == null) throw new IllegalArgumentException("OAuth state is invalid or expired");
        int separator = stateData.indexOf(':');
        if (separator <= 0 || separator == stateData.length() - 1)
            throw new IllegalArgumentException("OAuth state is invalid or expired; request a new authorization URL");
        String profileId = stateData.substring(0, separator);
        String codeVerifier = stateData.substring(separator + 1);
        if (providerError != null && !providerError.isBlank()) throw new IllegalArgumentException("TikTok authorization was declined");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Authorization code is required");
        requireConfiguration();

        try {
            JsonNode token = postForm("https://open.tiktokapis.com/v2/oauth/token/", "client_key", clientKey,
                    "client_secret", clientSecret, "code", code, "grant_type", "authorization_code",
                    "redirect_uri", redirectUri, "code_verifier", codeVerifier);
            String accessToken = requiredText(token, "access_token");
            String openId = requiredText(token, "open_id");
            long expiresIn = token.path("expires_in").asLong(0);
            if (expiresIn <= 0) throw new IllegalStateException("TikTok returned an invalid access token expiry");
            String refreshToken = textOrNull(token, "refresh_token");
            long refreshExpiresIn = token.path("refresh_expires_in").asLong(0);
            JsonNode userInfo = getJson("https://open.tiktokapis.com/v2/user/info/?fields=open_id,display_name,avatar_url", accessToken);
            JsonNode user = userInfo.path("data").path("user");
            String displayName = user.path("display_name").asText(openId);
            String expiresScope = textOrNull(token, "scope");
            if (expiresScope == null || expiresScope.isBlank()) expiresScope = SCOPES;

            ResellerProfile owner = profiles.findById(UUID.fromString(profileId))
                    .orElseThrow(() -> new IllegalArgumentException("Reseller profile no longer exists"));
            SocialAccountOAuthCredential existing = credentials.findByProviderAndProviderUserId(PROVIDER, openId).orElse(null);
            SocialAccount account;
            SocialAccountOAuthCredential credential;
            if (existing != null) {
                account = existing.getSocialAccount();
                if (!account.getReseller().getId().equals(owner.getId()))
                    throw new IllegalArgumentException("This TikTok account is already linked to another reseller");
                credential = existing;
            } else {
                String username = uniqueUsername(owner.getId(), displayName, openId);
                account = accounts.save(SocialAccount.builder().reseller(owner).platform(PROVIDER).username(username)
                        .metricsSource("API").status("ACTIVE").build());
                credential = SocialAccountOAuthCredential.builder().socialAccount(account).provider(PROVIDER).providerUserId(openId).build();
            }
            Instant now = Instant.now();
            credential.setAccessTokenCiphertext(cipher.encrypt(accessToken));
            credential.setRefreshTokenCiphertext(refreshToken == null ? null : cipher.encrypt(refreshToken));
            credential.setTokenExpiresAt(now.plusSeconds(expiresIn));
            credential.setRefreshTokenExpiresAt(refreshExpiresIn > 0 ? now.plusSeconds(refreshExpiresIn) : null);
            credential.setScopes(expiresScope);
            credentials.save(credential);
            return TikTokConnectedAccountResponse.builder().socialAccountId(account.getId()).platform(PROVIDER)
                    .username(account.getUsername()).displayName(displayName).status(account.getStatus())
                    .tokenExpiresAt(credential.getTokenExpiresAt()).scopes(expiresScope.split(",")).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TikTok OAuth request was interrupted");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not communicate with TikTok OAuth API");
        }
    }

    private String uniqueUsername(UUID resellerId, String displayName, String openId) {
        String base = displayName == null || displayName.isBlank() ? "TikTok user" : displayName;
        base = base.substring(0, Math.min(135, base.length()));
        if (accounts.findByResellerIdAndPlatformAndUsername(resellerId, PROVIDER, base).isEmpty()) return base;
        String suffix = "_" + openId.substring(Math.max(0, openId.length() - 10));
        return base.substring(0, Math.min(base.length(), 150 - suffix.length())) + suffix;
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable for TikTok PKCE");
        }
    }

    private JsonNode postForm(String url, String... values) throws java.io.IOException, InterruptedException {
        StringBuilder form = new StringBuilder();
        for (int i = 0; i < values.length; i += 2) {
            if (!form.isEmpty()) form.append('&');
            form.append(URLEncoder.encode(values[i], StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(values[i + 1], StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.toString())).build();
        return parseSuccessful(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private JsonNode getJson(String url, String accessToken) throws java.io.IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken).GET().build();
        return parseSuccessful(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private JsonNode parseSuccessful(HttpResponse<String> response) throws java.io.IOException {
        JsonNode body = mapper.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || body.hasNonNull("error") && !"".equals(body.path("error").asText()))
            throw new IllegalStateException("TikTok OAuth API rejected the request");
        return body;
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null || value.isBlank()) throw new IllegalStateException("TikTok response is missing " + field);
        return value;
    }
    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
    private void requireConfiguration() {
        if (clientKey.isBlank() || clientSecret.isBlank() || redirectUri.isBlank())
            throw new IllegalStateException("Configure TIKTOK_CLIENT_KEY, TIKTOK_CLIENT_SECRET and TIKTOK_REDIRECT_URI");
    }
    private ResellerProfile currentActiveReseller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user))
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        boolean role = auth.getAuthorities().stream().anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()));
        if (!user.isEnabled() || !role) throw new AccessDeniedException("Reseller role required");
        ResellerProfile profile = profiles.findByUserId(user.getId()).orElseThrow(() -> new AccessDeniedException("Reseller profile required"));
        if (!"ACTIVE".equals(profile.getStatus())) throw new AccessDeniedException("Active reseller profile required");
        return profile;
    }
}
