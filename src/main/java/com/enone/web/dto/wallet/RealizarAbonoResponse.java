package com.enone.web.dto.wallet;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealizarAbonoResponse {
    private Boolean success;
    private String mensaje;
    private String transactionId; // ID de la transacción del banco
    private BigDecimal nuevoSaldo;
}