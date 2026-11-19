package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.ResellerScoreRankingResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokVideoItemResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokVideoListResponse;
import com.aitravel.reseller.entity.SocialAccountOAuthCredential;
import com.aitravel.reseller.repository.SocialAccountOAuthCredentialRepository;
import com.aitravel.reseller.service.reseller.TikTokVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResellerScoreRankingService {
    private final SocialAccountOAuthCredentialRepository credentials;
    private final TikTokVideoService tikTokVideos;

    public ResellerScoreRankingResponse ranking() {
        Map<UUID, Totals> byReseller = new HashMap<>();
        for (SocialAccountOAuthCredential credential : credentials.findActiveForRanking("TIKTOK")) {
            var reseller = credential.getSocialAccount().getReseller();
            Totals totals = byReseller.computeIfAbsent(reseller.getId(),
                    id -> new Totals(id, reseller.getUser().getFullName()));
            Set<String> seenVideos = new HashSet<>();
            Set<Long> seenCursors = new HashSet<>();
            Long cursor = null;
            while (true) {
                TikTokVideoListResponse page = tikTokVideos.listVideosForConnectedAccount(credential, 20, cursor);
                for (TikTokVideoItemResponse video : page.getVideos()) {
                    if (video.getId() == null || video.getLikeCount() == null
                            || video.getShareCount() == null || video.getCommentCount() == null) {
                        throw new IllegalStateException("TikTok returned a video without engagement metrics");
                    }
                    if (seenVideos.add(video.getId())) {
                        totals.likes = Math.addExact(totals.likes, video.getLikeCount());
                        totals.shares = Math.addExact(totals.shares, video.getShareCount());
                        totals.comments = Math.addExact(totals.comments, video.getCommentCount());
                        totals.videoCount++;
                    }
                }
                if (!page.isHasMore()) break;
                if (page.getCursor() == null || !seenCursors.add(page.getCursor())) {
                    throw new IllegalStateException("TikTok pagination stopped before all videos were read");
                }
                cursor = page.getCursor();
            }
        }

        List<Totals> sorted = new ArrayList<>(byReseller.values());
        sorted.sort(Comparator.comparingLong(Totals::points).reversed()
                .thenComparing(t -> t.resellerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(t -> t.resellerId));
        List<ResellerScoreRankingResponse.Entry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Totals t = sorted.get(i);
            entries.add(new ResellerScoreRankingResponse.Entry(i + 1, t.resellerId, t.resellerName,
                    t.videoCount, t.likes, t.shares, t.comments, t.points()));
        }
        return new ResellerScoreRankingResponse(Instant.now(), entries);
    }

    private static class Totals {
        private final UUID resellerId;
        private final String resellerName;
        private long videoCount;
        private long likes;
        private long shares;
        private long comments;

        private Totals(UUID resellerId, String resellerName) {
            this.resellerId = resellerId;
            this.resellerName = resellerName;
        }

        private long points() {
            return Math.addExact(likes, Math.addExact(Math.multiplyExact(2, shares),
                    Math.multiplyExact(3, comments)));
        }
    }
}
