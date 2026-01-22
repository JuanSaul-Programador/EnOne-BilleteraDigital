package com.enone.web.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto mínimo de depósito es S/ 1.00")
    private BigDecimal amount;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;
}