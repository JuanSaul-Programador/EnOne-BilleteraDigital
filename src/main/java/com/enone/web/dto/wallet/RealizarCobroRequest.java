package com.enone.web.dto.wallet;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealizarCobroRequest {
    private String numeroTarjeta;
    private BigDecimal monto;
}