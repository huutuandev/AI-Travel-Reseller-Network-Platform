package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.respone.reseller.TikTokVideoItemResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokVideoListResponse;
import com.aitravel.reseller.entity.ResellerProfile;
import com.aitravel.reseller.entity.SocialAccount;
import com.aitravel.reseller.entity.SocialAccountOAuthCredential;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.SocialAccountOAuthCredentialRepository;
import com.aitravel.reseller.service.merchant.ResellerScoreRankingService;
import com.aitravel.reseller.service.reseller.TikTokVideoService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResellerScoreRankingServiceTest {
    private final SocialAccountOAuthCredentialRepository credentials = mock(SocialAccountOAuthCredentialRepository.class);
    private final TikTokVideoService videos = mock(TikTokVideoService.class);
    private final ResellerScoreRankingService service = new ResellerScoreRankingService(credentials, videos);

    @Test
    void sumsAllPagesAndAccountsAndOrdersByWeightedPoints() {
        UUID firstId = UUID.randomUUID(), secondId = UUID.randomUUID();
        var accountA = account(firstId, "Mia");
        var accountB = account(firstId, "Mia");
        var accountC = account(secondId, "Sam");
        when(credentials.findActiveForRanking("TIKTOK")).thenReturn(List.of(accountA, accountB, accountC));
        when(videos.listVideosForConnectedAccount(accountA, 20, null))
                .thenReturn(page(true, 17L, video("1", 10, 2, 3)));
        when(videos.listVideosForConnectedAccount(accountA, 20, 17L))
                .thenReturn(page(false, null, video("1", 10, 2, 3), video("2", 4, 1, 1)));
        when(videos.listVideosForConnectedAccount(accountB, 20, null))
                .thenReturn(page(false, null, video("3", 1, 0, 2)));
        when(videos.listVideosForConnectedAccount(accountC, 20, null))
                .thenReturn(page(false, null, video("4", 20, 2, 6)));

        var ranking = service.ranking().rankings();

        assertEquals(2, ranking.size());
        assertEquals(secondId, ranking.get(0).resellerId());
        assertEquals(42, ranking.get(0).points());
        assertEquals(1, ranking.get(0).rank());
        assertEquals(firstId, ranking.get(1).resellerId());
        assertEquals(15, ranking.get(1).likes());
        assertEquals(3, ranking.get(1).shares());
        assertEquals(6, ranking.get(1).comments());
        assertEquals(39, ranking.get(1).points());
        assertEquals(3, ranking.get(1).videoCount());
    }

    @Test
    void refusesIncompletePaginationInsteadOfPublishingPartialRanks() {
        var account = account(UUID.randomUUID(), "Mia");
        when(credentials.findActiveForRanking("TIKTOK")).thenReturn(List.of(account));
        when(videos.listVideosForConnectedAccount(account, 20, null))
                .thenReturn(page(true, null, video("1", 1, 1, 1)));
        assertThrows(IllegalStateException.class, service::ranking);
    }

    private SocialAccountOAuthCredential account(UUID resellerId, String name) {
        var reseller = ResellerProfile.builder().id(resellerId).user(User.builder().fullName(name).build()).build();
        return SocialAccountOAuthCredential.builder()
                .socialAccount(SocialAccount.builder().id(UUID.randomUUID()).reseller(reseller).build()).build();
    }

    private TikTokVideoListResponse page(boolean more, Long cursor, TikTokVideoItemResponse... items) {
        return TikTokVideoListResponse.builder().videos(List.of(items)).hasMore(more).cursor(cursor).build();
    }

    private TikTokVideoItemResponse video(String id, long likes, long shares, long comments) {
        return TikTokVideoItemResponse.builder().id(id).likeCount(likes).shareCount(shares)
                .commentCount(comments).build();
    }
}
