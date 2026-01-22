package com.enone.web.dto.wallet;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealizarCobroResponse {
    private Boolean success;
    private String mensaje;
    private String transactionId;
    private BigDecimal nuevoSaldo;
}