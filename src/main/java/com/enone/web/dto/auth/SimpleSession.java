package com.enone.web.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimpleSession {
    
    @NotNull(message = "El ID de sesión es obligatorio")
    private Long sessionId;
}