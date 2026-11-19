package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.CampaignResponse;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.reseller.ResellerCampaignRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerCampaignService {

    private final ResellerCampaignRepository repository;

    // Lấy tất cả chiến dịch
    public List<CampaignResponse> getAllCampaigns() {

        return repository.findAllCampaigns();
    }

    // Lấy chi tiết chiến dịch
    public CampaignResponse getCampaignDetail(UUID campaignId) {

        if (campaignId == null) {
            throw new IllegalArgumentException(
                    "Campaign ID must not be null");
        }

        return repository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found"));
    }
}