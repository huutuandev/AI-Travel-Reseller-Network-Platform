package com.aitravel.reseller.service.chat;

import com.aitravel.reseller.dto.chat.ChatSessionContext;

public interface ChatTrackingContextResolver {

    /**
     * Resolves a tracking code to the validated merchant, product, and active
     * knowledge-document scope for a new chat session.
     *
     * @param trackingCode value from tracking_links.tracking_code
     * @return validated server-side chat context
     */
    ChatSessionContext resolve(String trackingCode);
}