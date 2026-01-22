package com.enone.web.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidarTarjetaResponse {
    
    private Boolean success;
    private String mensaje;
    private String numeroTarjetaEnmascarado;
    private String holderName;
}