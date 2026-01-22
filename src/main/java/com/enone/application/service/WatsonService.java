package com.enone.application.service;

import com.enone.web.dto.chat.ChatResponse;

public interface WatsonService {
    ChatResponse sendMessage(Long userId, String messageText, String sessionId);

    boolean isConfigured();
}
