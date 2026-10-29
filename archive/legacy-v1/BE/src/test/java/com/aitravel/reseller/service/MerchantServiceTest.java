package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.RegisterMerchantRequest;
import com.aitravel.reseller.dto.request.UpdateMerchantRequest;
import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MerchantServiceImpl merchantService;

    private UUID userId;
    private User mockUser;
    private Merchant mockMerchant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .status("ACTIVE")
                .roles(Set.of(Role.builder().name("ROLE_RESELLER").build()))
                .build();

        mockMerchant = Merchant.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .name("Sun World Da Nang")
                .phone("0901234567")
                .address("Da Nang")
                .website("https://sunworld.vn")
                .logoUrl("https://sunworld.vn/logo.png")
                .verificationStatus("PENDING")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "user@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_RESELLER"))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. User đã đăng nhập onboarding Merchant thành công (POST /merchant/register)")
    void testRegisterMerchant_Success() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .name("Sun World Da Nang")
                .phone("0901234567")
                .address("Da Nang")
                .website("https://sunworld.vn")
                .logoUrl("https://sunworld.vn/logo.png")
                .build();

        when(merchantRepository.existsByUser_Id(userId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            m.setId(mockMerchant.getId());
            return m;
        });

        MerchantResponse response = merchantService.registerMerchant(request);

        assertNotNull(response);
        assertEquals("Sun World Da Nang", response.getName());
        assertEquals("0901234567", response.getPhone());
        assertEquals("PENDING", response.getVerificationStatus());
        assertEquals(userId, response.getUserId());

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository, times(1)).save(captor.capture());
        Merchant saved = captor.getValue();
        assertEquals(mockUser, saved.getUser());
        assertEquals("PENDING", saved.getVerificationStatus());
    }

    @Test
    @DisplayName("2. Từ chối đăng ký Merchant trùng lặp nếu User đã sở hữu Merchant profile (409 Conflict)")
    void testRegisterMerchant_Duplicate_ThrowsConflict() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .name("Duplicate Merchant")
                .build();

        when(merchantRepository.existsByUser_Id(userId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> merchantService.registerMerchant(request));

        assertTrue(ex.getMessage().contains("User already has a registered merchant profile"));
        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    @DisplayName("3. Lấy thông tin Merchant của User hiện tại thành công")
    void testGetCurrentMerchant_Success() {
        when(merchantRepository.findByUser_Id(userId)).thenReturn(Optional.of(mockMerchant));

        MerchantResponse response = merchantService.getCurrentMerchant();

        assertNotNull(response);
        assertEquals(mockMerchant.getId(), response.getId());
        assertEquals("Sun World Da Nang", response.getName());
    }

    @Test
    @DisplayName("4. User chưa có Merchant profile khi gọi getCurrentMerchant sẽ ném ResourceNotFoundException (404)")
    void testGetCurrentMerchant_NotFound_ThrowsException() {
        when(merchantRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> merchantService.getCurrentMerchant());
    }

    @Test
    @DisplayName("5. Cập nhật thông tin Merchant hiện tại thành công")
    void testUpdateCurrentMerchant_Success() {
        when(merchantRepository.findByUser_Id(userId)).thenReturn(Optional.of(mockMerchant));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMerchantRequest request = UpdateMerchantRequest.builder()
                .name("Sun World Updated")
                .phone("0988888888")
                .address("Ha Noi")
                .website("https://updated.vn")
                .logoUrl("https://updated.vn/logo.png")
                .build();

        MerchantResponse response = merchantService.updateCurrentMerchant(request);

        assertNotNull(response);
        assertEquals("Sun World Updated", response.getName());
        assertEquals("0988888888", response.getPhone());
    }

    @Test
    @DisplayName("6. Xác thực quyền sở hữu Merchant: thành công cho chủ sở hữu, từ chối người khác")
    void testValidateOwnership() {
        when(merchantRepository.findByUser_Id(userId)).thenReturn(Optional.of(mockMerchant));

        // Chủ sở hữu hợp lệ
        assertDoesNotThrow(() -> merchantService.validateOwnership(mockMerchant.getId()));

        // Khác ID merchant -> từ chối
        UUID otherMerchantId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> merchantService.validateOwnership(otherMerchantId));
    }

    @Test
    @DisplayName("7. User cũ có ROLE_RESELLER sau migration vẫn giữ nguyên Merchant profile và thao tác bình thường")
    void testLegacyUserAfterMigration_WorksNormally() {
        // Tài khoản cũ có cả ROLE_MERCHANT và ROLE_RESELLER hoặc chỉ ROLE_RESELLER sau migration
        User legacyUser = User.builder()
                .id(userId)
                .email("legacy_merchant@example.com")
                .roles(Set.of(
                        Role.builder().name("ROLE_RESELLER").build(),
                        Role.builder().name("ROLE_MERCHANT").build()
                ))
                .status("ACTIVE")
                .build();

        when(merchantRepository.findByUser_Id(userId)).thenReturn(Optional.of(mockMerchant));

        Merchant entity = merchantService.getCurrentMerchantEntity();
        assertNotNull(entity);
        assertEquals(mockMerchant.getId(), entity.getId());
    }
}
