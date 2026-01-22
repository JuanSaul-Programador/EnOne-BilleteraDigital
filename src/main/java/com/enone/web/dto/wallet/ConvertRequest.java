package com.enone.web.dto.wallet;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConvertRequest {
    
    @NotBlank(message = "La moneda origen es obligatoria")
    @Pattern(regexp = "^(PEN|USD|EUR)$", message = "Moneda origen inválida. Debe ser PEN, USD o EUR")
    private String fromCurrency;
    
    @NotBlank(message = "La moneda destino es obligatoria")
    @Pattern(regexp = "^(PEN|USD|EUR)$", message = "Moneda destino inválida. Debe ser PEN, USD o EUR")
    private String toCurrency;
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto mínimo de conversión es 1.00")
    private BigDecimal amount;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;
}