package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.CreateProductRequest;
import com.aitravel.reseller.dto.request.UpdateProductRequest;
import com.aitravel.reseller.dto.respone.ProductResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Merchant mockMerchant;
    private Product mockProduct;
    private UUID merchantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        productId = UUID.randomUUID();

        mockMerchant = Merchant.builder()
                .id(merchantId)
                .name("Sun World")
                .verificationStatus("PENDING")
                .build();

        mockProduct = Product.builder()
                .id(productId)
                .merchant(mockMerchant)
                .name("Vé Cáp Treo")
                .salePrice(new BigDecimal("800000.00"))
                .originalPrice(new BigDecimal("900000.00"))
                .status("ACTIVE")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. User có Merchant profile tạo sản phẩm thành công với status mặc định ACTIVE")
    void testCreateProduct_ImmediateCreation_DefaultsToActive() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Vé Cáp Treo")
                .salePrice(new BigDecimal("800000.00"))
                .originalPrice(new BigDecimal("900000.00"))
                .category("VE_THAM_QUAN")
                .build();

        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(productId, response.getId());
        assertEquals("Vé Cáp Treo", response.getName());
        assertEquals("ACTIVE", response.getStatus());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("2. Client không thể tự chỉ định status khi tạo sản phẩm (bị từ chối)")
    void testCreateProduct_ClientSuppliedStatus_ThrowsException() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Vé Cáp Treo")
                .salePrice(new BigDecimal("800000.00"))
                .status("ACTIVE")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(request));

        assertTrue(ex.getMessage().contains("Client cannot specify product status"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("3. User chưa có Merchant profile không thể tạo sản phẩm (bị chặn 403)")
    void testCreateProduct_WithoutMerchantProfile_ThrowsAccessDenied() {
        when(merchantService.getCurrentMerchantEntity())
                .thenThrow(new AccessDeniedException("User does not have an onboarded merchant profile"));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("Vé Không Hồ Sơ")
                .salePrice(new BigDecimal("500000.00"))
                .build();

        assertThrows(AccessDeniedException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("4. Merchant cập nhật sản phẩm của mình trực tiếp và giữ nguyên trạng thái ACTIVE")
    void testUpdateProduct_MaintainsActiveStatus() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Vé Cáp Treo Cập Nhật")
                .salePrice(new BigDecimal("850000.00"))
                .build();

        ProductResponse response = productService.updateProduct(productId, request);

        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Vé Cáp Treo Cập Nhật", response.getName());
        verify(productRepository, times(1)).save(mockProduct);
    }

    @Test
    @DisplayName("5. Client không thể tự gửi status trong update request")
    void testUpdateProduct_ClientSuppliedStatus_ThrowsException() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Vé Cáp Treo")
                .salePrice(new BigDecimal("800000.00"))
                .status("DRAFT")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(productId, request));

        assertTrue(ex.getMessage().contains("Client cannot specify product status directly"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("6. Merchant không thể cập nhật sản phẩm của Merchant khác (403 Forbidden)")
    void testUpdateProduct_NotOwner_ThrowsAccessDenied() {
        Merchant anotherMerchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Another Merchant")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(anotherMerchant);

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Sửa lậu")
                .salePrice(new BigDecimal("700000.00"))
                .build();

        assertThrows(AccessDeniedException.class, () -> productService.updateProduct(productId, request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("7. Không thể cập nhật sản phẩm đã bị ARCHIVED (409 Conflict)")
    void testUpdateProduct_ArchivedProduct_ThrowsIllegalState() {
        Product archivedProduct = Product.builder()
                .id(productId)
                .merchant(mockMerchant)
                .status("ARCHIVED")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(archivedProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Sửa sp đã xóa")
                .salePrice(new BigDecimal("700000.00"))
                .build();

        assertThrows(IllegalStateException.class, () -> productService.updateProduct(productId, request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("8. Xóa sản phẩm thực hiện soft-delete (status = ARCHIVED)")
    void testDeleteProduct_SoftDeleteSuccess() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        productService.deleteProduct(productId);

        assertEquals("ARCHIVED", mockProduct.getStatus());
        verify(productRepository, times(1)).save(mockProduct);
    }

    @Test
    @DisplayName("9. Merchant không thể xóa sản phẩm của Merchant khác (403 Forbidden)")
    void testDeleteProduct_NotOwner_ThrowsAccessDenied() {
        Merchant anotherMerchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Another Merchant")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(anotherMerchant);

        assertThrows(AccessDeniedException.class, () -> productService.deleteProduct(productId));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("10. Sản phẩm ACTIVE hiển thị công khai qua getProductById")
    void testGetPublicProduct_Active_Visible() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        ProductResponse response = productService.getProductById(productId);

        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    @DisplayName("11. Sản phẩm non-active (ARCHIVED / DRAFT cũ) bị ẩn khỏi người dùng công khai (404)")
    void testGetProductById_NonActive_HiddenFromPublic() {
        Product draftProduct = Product.builder()
                .id(productId)
                .merchant(mockMerchant)
                .status("DRAFT")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(draftProduct));

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(productId));
    }

    @Test
    @DisplayName("12. Chủ sở hữu Merchant vẫn xem được sản phẩm non-active của chính mình")
    void testGetProductById_NonActive_VisibleToOwner() {
        Product draftProduct = Product.builder()
                .id(productId)
                .merchant(mockMerchant)
                .status("DRAFT")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(draftProduct));
        when(merchantService.getCurrentMerchantEntity()).thenReturn(mockMerchant);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_RESELLER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductResponse response = productService.getProductById(productId);
        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    @DisplayName("13. Danh sách công khai chỉ trả về sản phẩm ACTIVE")
    void testGetPublicProducts_OnlyActiveProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Product activeProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Vé Đã Kích Hoạt")
                .salePrice(new BigDecimal("500000.00"))
                .status("ACTIVE")
                .build();

        when(productRepository.findByStatus("ACTIVE", pageable))
                .thenReturn(new PageImpl<>(List.of(activeProduct)));

        Page<ProductResponse> result = productService.getPublicProducts(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("ACTIVE", result.getContent().get(0).getStatus());
        verify(productRepository, times(1)).findByStatus("ACTIVE", pageable);
    }
}
