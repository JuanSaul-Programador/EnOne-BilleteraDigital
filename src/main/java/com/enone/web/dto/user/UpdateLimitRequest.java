package com.enone.web.dto.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLimitRequest {
    
    @NotNull(message = "El nuevo límite es obligatorio")
    @DecimalMin(value = "100.00", message = "El límite mínimo es S/ 100.00")
    private BigDecimal newLimit;
}