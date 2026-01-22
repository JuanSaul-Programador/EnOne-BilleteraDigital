package com.enone.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    
    @NotBlank(message = "El código de confirmación es obligatorio")
    @Pattern(regexp = "^[A-Z0-9]{8}$", message = "El código debe tener exactamente 8 caracteres alfanuméricos")
    private String code;
}