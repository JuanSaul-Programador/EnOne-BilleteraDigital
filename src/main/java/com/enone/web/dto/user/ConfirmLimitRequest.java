package com.enone.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConfirmLimitRequest {
    
    @NotBlank(message = "El código de confirmación es obligatorio")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "El código debe tener exactamente 6 caracteres alfanuméricos")
    private String code;
}