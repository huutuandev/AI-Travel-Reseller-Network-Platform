package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.respone.JoinCampaignResponse;
import com.aitravel.reseller.entity.Campaign;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.entity.Reseller;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.TrackingLink;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ForbiddenException;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.CampaignRepository;
import com.aitravel.reseller.repository.ResellerRepository;
import com.aitravel.reseller.repository.TrackingLinkRepository;
import com.aitravel.reseller.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private ResellerRepository resellerRepository;

    @Mock
    private TrackingLinkRepository trackingLinkRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CampaignService campaignService;

    private User activeUser;
    private Reseller activeReseller;
    private Campaign activeCampaign;
    private Product product;
    private UUID campaignId;
    private String userEmail = "reseller@test.com";

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        
        Role resellerRole = new Role();
        resellerRole.setName("RESELLER");

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .email(userEmail)
                .fullName("Test User")
                .status("ACTIVE")
                .roles(Set.of(resellerRole))
                .build();

        activeReseller = Reseller.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .status("ACTIVE")
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .build();

        activeCampaign = Campaign.builder()
                .id(campaignId)
                .product(product)
                .status("ACTIVE")
                .commissionRate(new BigDecimal("10.00"))
                .resellerSplitRate(new BigDecimal("60.00"))
                .build();
    }

    @Test
    void joinCampaign_Success() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign));
        when(trackingLinkRepository.findByCampaignIdAndResellerId(campaignId, activeReseller.getId())).thenReturn(Optional.empty());
        
        TrackingLink savedLink = TrackingLink.builder()
                .campaign(activeCampaign)
                .product(product)
                .trackingCode("TEST_123")
                .landingSlug("/r/test-123")
                .commissionSnapshot(new BigDecimal("10.00"))
                .resellerSplitSnapshot(new BigDecimal("60.00"))
                .build();
                
        when(trackingLinkRepository.saveAndFlush(any(TrackingLink.class))).thenReturn(savedLink);

        JoinCampaignResponse response = campaignService.joinCampaign(campaignId, userEmail);

        assertNotNull(response);
        assertEquals("Tham gia chiến dịch thành công", response.getMessage());
        assertEquals(new BigDecimal("10.00"), response.getCommissionSnapshot());
        assertEquals(new BigDecimal("60.00"), response.getResellerSplitSnapshot());
        
        ArgumentCaptor<TrackingLink> captor = ArgumentCaptor.forClass(TrackingLink.class);
        verify(trackingLinkRepository).saveAndFlush(captor.capture());
        
        TrackingLink capturedLink = captor.getValue();
        assertEquals(activeCampaign.getCommissionRate(), capturedLink.getCommissionSnapshot());
        assertEquals(activeCampaign.getResellerSplitRate(), capturedLink.getResellerSplitSnapshot());
        assertEquals(activeCampaign.getProduct(), capturedLink.getProduct());
    }

    @Test
    void joinCampaign_AlreadyJoined_ReturnsExistingLink() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign));
        
        TrackingLink existingLink = TrackingLink.builder()
                .campaign(activeCampaign)
                .product(product)
                .trackingCode("OLD_CODE")
                .landingSlug("/r/old-slug")
                .commissionSnapshot(new BigDecimal("5.00"))
                .resellerSplitSnapshot(new BigDecimal("50.00"))
                .build();
                
        when(trackingLinkRepository.findByCampaignIdAndResellerId(campaignId, activeReseller.getId()))
                .thenReturn(Optional.of(existingLink));

        JoinCampaignResponse response = campaignService.joinCampaign(campaignId, userEmail);

        assertNotNull(response);
        assertEquals("Bạn đã tham gia chiến dịch này", response.getMessage());
        assertEquals(new BigDecimal("5.00"), response.getCommissionSnapshot());
        assertEquals(new BigDecimal("50.00"), response.getResellerSplitSnapshot());
        
        verify(trackingLinkRepository, never()).saveAndFlush(any(TrackingLink.class));
    }

    @Test
    void joinCampaign_ConcurrentJoin_ReturnsExistingLink() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign));
        
        when(trackingLinkRepository.findByCampaignIdAndResellerId(campaignId, activeReseller.getId()))
                .thenReturn(Optional.empty()) 
                .thenReturn(Optional.of(TrackingLink.builder()
                        .campaign(activeCampaign)
                        .product(product)
                        .commissionSnapshot(new BigDecimal("10.00"))
                        .resellerSplitSnapshot(new BigDecimal("60.00"))
                        .build())); 
                        
        when(trackingLinkRepository.saveAndFlush(any(TrackingLink.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        JoinCampaignResponse response = campaignService.joinCampaign(campaignId, userEmail);

        assertNotNull(response);
        assertEquals("Bạn đã tham gia chiến dịch này", response.getMessage());
    }

    @Test
    void joinCampaign_NotResellerRole_ThrowsForbiddenException() {
        activeUser.setRoles(Set.of()); // No roles
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));

        assertThrows(ForbiddenException.class, () -> campaignService.joinCampaign(campaignId, userEmail));
    }

    @Test
    void joinCampaign_ResellerSuspended_ThrowsForbiddenException() {
        activeReseller.setStatus("SUSPENDED");
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));

        assertThrows(ForbiddenException.class, () -> campaignService.joinCampaign(campaignId, userEmail));
    }

    @Test
    void joinCampaign_CampaignNotActive_ThrowsIllegalStateException() {
        activeCampaign.setStatus("DRAFT");
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign));

        assertThrows(IllegalStateException.class, () -> campaignService.joinCampaign(campaignId, userEmail));
    }

    @Test
    void joinCampaign_CampaignNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(activeUser));
        when(resellerRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(activeReseller));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> campaignService.joinCampaign(campaignId, userEmail));
    }
}
