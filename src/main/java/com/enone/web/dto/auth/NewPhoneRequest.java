package com.enone.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NewPhoneRequest {
    
    @NotBlank(message = "El nuevo teléfono es obligatorio")
    @Pattern(regexp = "^\\+51[0-9]{9}$", message = "El teléfono debe tener el formato +51XXXXXXXXX")
    private String newPhone;
}