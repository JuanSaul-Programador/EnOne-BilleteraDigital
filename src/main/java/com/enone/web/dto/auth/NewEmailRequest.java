package com.enone.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewEmailRequest {
    
    @NotBlank(message = "El nuevo email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 120, message = "El email no puede exceder 120 caracteres")
    private String newEmail;
}