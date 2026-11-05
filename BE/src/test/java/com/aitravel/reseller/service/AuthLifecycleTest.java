package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.*;
import com.aitravel.reseller.dto.respone.AuthResponse;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.jwt.JwtUtil;
import com.aitravel.reseller.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisService redisService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User activeUser;
    private User inactiveUser;
    private Role resellerRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);

        userId = UUID.randomUUID();
        resellerRole = Role.builder().id(UUID.randomUUID()).name("ROLE_RESELLER").build();

        activeUser = User.builder()
                .id(userId)
                .email("active@example.com")
                .passwordHash("hashed_pwd")
                .status("ACTIVE")
                .roles(Set.of(resellerRole))
                .build();

        inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@example.com")
                .passwordHash("hashed_pwd")
                .status("INACTIVE")
                .roles(Set.of(resellerRole))
                .build();
    }

    @Test
    @DisplayName("5a. Xác thực OTP email kích hoạt tài khoản thành ACTIVE thành công")
    void testVerifyEmail_Success() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("inactive@example.com")
                .otp("123456")
                .build();

        when(redisService.get("auth:email:verify:inactive@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        authService.verifyEmail(request);

        assertEquals("ACTIVE", inactiveUser.getStatus());
        verify(userRepository, times(1)).save(inactiveUser);
        verify(redisService, times(1)).delete("auth:email:verify:inactive@example.com");
    }

    @Test
    @DisplayName("5b. Đăng nhập thành công trả về JWT accessToken và refreshToken")
    void testLogin_ActiveUser_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("active@example.com")
                .password("Password@123")
                .build();

        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));

        CustomUserDetails userDetails = new CustomUserDetails(
                userId, "active@example.com", List.of(new SimpleGrantedAuthority("ROLE_RESELLER"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtil.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock_jwt_access_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_jwt_access_token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("5c. Tài khoản INACTIVE không thể đăng nhập trước khi xác thực OTP")
    void testLogin_InactiveUser_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@example.com")
                .password("Password@123")
                .build();

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Account is not verified"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("15a. Làm mới JWT bằng refreshToken hợp lệ")
    void testRefreshToken_Success() {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = userId + ":" + tokenId;
        TokenRefreshRequest request = TokenRefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        String redisKey = "auth:refresh:" + userId + ":" + tokenId;
        when(redisService.get(redisKey)).thenReturn("active@example.com");
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        verify(redisService, times(1)).delete(redisKey);
    }

    @Test
    @DisplayName("15b. Đăng xuất thu hồi refreshToken trong Redis")
    void testLogout_Success() {
        String tokenId = UUID.randomUUID().toString();
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(userId + ":" + tokenId)
                .build();

        authService.logout(request);

        verify(redisService, times(1)).delete("auth:refresh:" + userId + ":" + tokenId);
    }

    @Test
    @DisplayName("15c. Quên mật khẩu, xác thực OTP và đổi mật khẩu thành công")
    void testForgotPassword_VerifyOtp_And_ResetPassword_Success() {
        // Step 1: Quên mật khẩu
        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder()
                .email("active@example.com")
                .build();
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(redisService.exists(anyString())).thenReturn(false);

        authService.forgotPassword(forgotReq);
        verify(emailService, times(1)).sendPasswordResetOtpEmail(eq("active@example.com"), anyString());

        // Step 2: Xác thực OTP reset
        VerifyResetOtpRequest verifyReq = VerifyResetOtpRequest.builder()
                .email("active@example.com")
                .otp("654321")
                .build();
        when(redisService.get("auth:password-reset:active@example.com")).thenReturn("654321");

        String resetToken = authService.verifyResetOtp(verifyReq);
        assertNotNull(resetToken);

        // Step 3: Đặt mật khẩu mới
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .resetToken(resetToken)
                .newPassword("BrandNewPassword@123")
                .build();
        when(redisService.get("auth:password-reset:verified:" + resetToken)).thenReturn("active@example.com");
        when(passwordEncoder.encode("BrandNewPassword@123")).thenReturn("new_hashed_pwd");

        authService.resetPassword(resetReq);

        assertEquals("new_hashed_pwd", activeUser.getPasswordHash());
        verify(userRepository, times(1)).save(activeUser);
        verify(redisService, times(1)).deleteByPattern("auth:refresh:" + userId + ":*");
    }
}
