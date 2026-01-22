package com.enone.web.dto.wallet;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    
    @NotBlank(message = "El usuario destino es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre de usuario debe tener entre 3 y 120 caracteres")
    private String toUsername;
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto mínimo de transferencia es S/ 1.00")
    private BigDecimal amount;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;
    
    @Pattern(regexp = "^[0-9]{6}$", message = "El token 2FA debe tener exactamente 6 dígitos")
    private String token2fa;
    
    @NotBlank(message = "La moneda es obligatoria")
    @Pattern(regexp = "^(PEN|USD|EUR)$", message = "Moneda inválida. Debe ser PEN, USD o EUR")
    private String currency;
}