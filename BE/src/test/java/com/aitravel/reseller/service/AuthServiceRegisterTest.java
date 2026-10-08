package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.RegisterRequest;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.RoleRepository;
import com.aitravel.reseller.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisService redisService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Role resellerRole;

    @BeforeEach
    void setUp() {
        resellerRole = Role.builder().id(UUID.randomUUID()).name("ROLE_RESELLER").build();
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    }

    @Test
    @DisplayName("1. Đăng ký thành công gán ROLE_RESELLER, lưu fullName, status INACTIVE, gửi OTP email, không tạo Merchant profile")
    void testRegister_Success_CreatesRoleReseller_Inactive_SendsOtp() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password@123")
                .fullName("Nguyen Van A")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_RESELLER")).thenReturn(Optional.of(resellerRole));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals("Nguyen Van A", savedUser.getFullName());
        assertEquals("INACTIVE", savedUser.getStatus());
        assertEquals("hashed_password", savedUser.getPasswordHash());
        assertTrue(savedUser.getRoles().contains(resellerRole));
        assertEquals(1, savedUser.getRoles().size());

        // Xác nhận gửi OTP qua email
        verify(emailService, times(1)).sendOtpEmail(eq("newuser@example.com"), anyString());
    }

    @Test
    @DisplayName("2. Đăng ký công khai không thể gán ADMIN hoặc MERCHANT (chỉ gán duy nhất ROLE_RESELLER)")
    void testRegister_CannotAssignAdminOrMerchant() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password@123")
                .fullName("Test User")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_RESELLER")).thenReturn(Optional.of(resellerRole));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        boolean hasAdmin = savedUser.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean hasMerchant = savedUser.getRoles().stream().anyMatch(r -> "ROLE_MERCHANT".equals(r.getName()));
        boolean hasReseller = savedUser.getRoles().stream().anyMatch(r -> "ROLE_RESELLER".equals(r.getName()));

        assertFalse(hasAdmin, "Public registration must never assign ROLE_ADMIN");
        assertFalse(hasMerchant, "Public registration must never assign ROLE_MERCHANT");
        assertTrue(hasReseller, "Public registration must assign ROLE_RESELLER");
    }

    @Test
    @DisplayName("3. Đăng ký lại cho User đang INACTIVE (chưa xác thực OTP) sẽ cập nhật fullName, password và gửi lại OTP")
    void testRegister_ExistingInactiveUser_UpdatesAndSendsOtp() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("pending@example.com")
                .fullName("Old Name")
                .passwordHash("old_hash")
                .status("INACTIVE")
                .roles(Set.of(resellerRole))
                .build();

        RegisterRequest request = RegisterRequest.builder()
                .email("pending@example.com")
                .password("NewPassword@123")
                .fullName("Updated Name")
                .build();

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("ROLE_RESELLER")).thenReturn(Optional.of(resellerRole));

        authService.register(request);

        verify(userRepository, times(1)).save(existingUser);
        assertEquals("Updated Name", existingUser.getFullName());
        assertEquals("INACTIVE", existingUser.getStatus());
        assertEquals("hashed_password", existingUser.getPasswordHash());
        verify(emailService, times(1)).sendOtpEmail(eq("pending@example.com"), anyString());
    }

    @Test
    @DisplayName("4. Từ chối đăng ký nếu Email đã tồn tại và đang ACTIVE")
    void testRegister_ExistingActiveUser_ThrowsConflict() {
        User activeUser = User.builder()
                .id(UUID.randomUUID())
                .email("active@example.com")
                .fullName("Active User")
                .status("ACTIVE")
                .build();

        RegisterRequest request = RegisterRequest.builder()
                .email("active@example.com")
                .password("Password@123")
                .fullName("Active User")
                .build();

        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(roleRepository.findByName("ROLE_RESELLER")).thenReturn(Optional.of(resellerRole));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Email already exists and is active"));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("5. Từ chối đăng ký nếu tài khoản đã bị SUSPENDED")
    void testRegister_SuspendedUser_ThrowsConflict() {
        User suspendedUser = User.builder()
                .id(UUID.randomUUID())
                .email("suspended@example.com")
                .fullName("Suspended User")
                .status("SUSPENDED")
                .build();

        RegisterRequest request = RegisterRequest.builder()
                .email("suspended@example.com")
                .password("Password@123")
                .fullName("Suspended User")
                .build();

        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(suspendedUser));
        when(roleRepository.findByName("ROLE_RESELLER")).thenReturn(Optional.of(resellerRole));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Account is suspended"));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }
}
