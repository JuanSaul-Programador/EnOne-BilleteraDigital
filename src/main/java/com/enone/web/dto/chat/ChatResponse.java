package com.enone.web.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String message;
    private String sessionId;
    private String intentDetected; // Para debugging o lógica frontend
}
