package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.*;
import com.aitravel.reseller.dto.respone.AuthResponse;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.RoleRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${spring.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    private static final String OTP_PREFIX = "auth:email:verify:";
    private static final String OTP_RESEND_PREFIX = "auth:email:verify:resend:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final long OTP_TTL_MINUTES = 5;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if ("ACTIVE".equals(existingUser.getStatus())) {
                throw new IllegalStateException("Email already exists and is active");
            }
            if ("SUSPENDED".equals(existingUser.getStatus())) {
                throw new IllegalStateException("Account is suspended");
            }
            existingUser.setFullName(request.getFullName());
            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userRepository.save(existingUser);
        } else {
            Role resellerRole = roleRepository.findByName("ROLE_RESELLER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_RESELLER").description("Reseller Role").build()));

            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .status("INACTIVE")
                    .roles(Set.of(resellerRole))
                    .build();
            userRepository.save(newUser);
        }

        generateAndSendOtp(email);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail();
        String otpKey = OTP_PREFIX + email;
        String savedOtp = redisService.get(otpKey);

        if (savedOtp == null) {
            throw new IllegalArgumentException("OTP expired or not found");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Account is already verified and active");
        }

        user.setStatus("ACTIVE");
        userRepository.save(user);

        redisService.delete(otpKey);
        redisService.delete(OTP_RESEND_PREFIX + email);
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Account is already verified and active");
        }
        if ("SUSPENDED".equals(user.getStatus())) {
            throw new IllegalStateException("Account is suspended");
        }
        if (redisService.exists(OTP_RESEND_PREFIX + email)) {
            throw new IllegalStateException("Please wait before requesting a new OTP");
        }

        generateAndSendOtp(email);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

            if ("INACTIVE".equals(user.getStatus())) {
                throw new IllegalStateException("Account is not verified. Please verify your email first.");
            }
            if ("SUSPENDED".equals(user.getStatus())) {
                throw new IllegalStateException("Account is suspended.");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return generateTokens(userDetails);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }
    }

    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Refresh token format: {userId}:{tokenId}
        String[] parts = requestRefreshToken.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String userId = parts[0];
        String tokenId = parts[1];
        String redisKey = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;

        String email = redisService.get(redisKey);
        if (email == null) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Account is not active");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Rotate refresh token
        redisService.delete(redisKey);
        return generateTokens(userDetails);
    }

    private static final String RESET_OTP_PREFIX = "auth:password-reset:";
    private static final String RESET_OTP_RESEND_PREFIX = "auth:password-reset:resend:";
    private static final String RESET_TOKEN_PREFIX = "auth:password-reset:verified:";

    private static final long RESET_OTP_TTL_MINUTES = 5;
    private static final long RESET_TOKEN_TTL_MINUTES = 10;

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Security: Don't leak if email exists. Still pretend it succeeded if not.
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!"ACTIVE".equals(user.getStatus())) {
                // Return silently to avoid leak, or optionally throw an error if biz requires.
                // Best practice is to just return silently to not leak existence.
                return;
            }

            if (redisService.exists(RESET_OTP_RESEND_PREFIX + email)) {
                // Rate limited. Usually we would throw an exception, but returning silently also prevents leakage via timing.
                // For a better UX we can throw. Let's throw rate limit to be nice.
                throw new IllegalStateException("Please wait before requesting a new password reset OTP");
            }

            String otp = generate6DigitOtp();
            redisService.save(RESET_OTP_PREFIX + email, otp, RESET_OTP_TTL_MINUTES, TimeUnit.MINUTES);
            redisService.save(RESET_OTP_RESEND_PREFIX + email, "1", RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);

            emailService.sendPasswordResetOtpEmail(email, otp);
        }
    }

    public String verifyResetOtp(VerifyResetOtpRequest request) {
        String email = request.getEmail();
        String otpKey = RESET_OTP_PREFIX + email;
        String savedOtp = redisService.get(otpKey);

        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        // OTP is correct. Issue a reset token.
        String resetToken = UUID.randomUUID().toString();
        redisService.save(RESET_TOKEN_PREFIX + resetToken, email, RESET_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        // Clean up OTPs
        redisService.delete(otpKey);
        redisService.delete(RESET_OTP_RESEND_PREFIX + email);

        return resetToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String resetToken = request.getResetToken();
        String email = redisService.get(RESET_TOKEN_PREFIX + resetToken);

        if (email == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete reset token
        redisService.delete(RESET_TOKEN_PREFIX + resetToken);

        // Revoke all refresh tokens for this user
        redisService.deleteByPattern(REFRESH_TOKEN_PREFIX + user.getId() + ":*");
    }

    public void logout(LogoutRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        String[] parts = requestRefreshToken.split(":");
        if (parts.length == 2) {
            String redisKey = REFRESH_TOKEN_PREFIX + parts[0] + ":" + parts[1];
            redisService.delete(redisKey);
        }
    }

    private AuthResponse generateTokens(CustomUserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        String tokenId = UUID.randomUUID().toString();
        String refreshToken = userDetails.getId() + ":" + tokenId;

        String redisKey = REFRESH_TOKEN_PREFIX + userDetails.getId() + ":" + tokenId;
        redisService.save(redisKey, userDetails.getEmail(), refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    private void generateAndSendOtp(String email) {
        String otp = generate6DigitOtp();
        redisService.save(OTP_PREFIX + email, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisService.save(OTP_RESEND_PREFIX + email, "1", RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        emailService.sendOtpEmail(email, otp);
    }

    private String generate6DigitOtp() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000);
        return String.format("%06d", num);
    }
}
