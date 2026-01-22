package com.enone.web.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "10.00", message = "El monto mínimo de retiro es S/ 10.00")
    private BigDecimal amount;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;
}