package com.enone.web.dto.chat;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatRequest {
    @NotBlank(message = "El mensaje no puede estar vacío")
    private String message;

    private String sessionId; // Opcional, para mantener contexto
}
