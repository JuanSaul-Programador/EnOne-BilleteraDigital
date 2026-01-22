package com.enone.application.mapper.impl;


import com.enone.application.mapper.TarjetaMapper;
import com.enone.domain.model.MockTarjeta;
import com.enone.domain.model.UserTarjeta;
import com.enone.web.dto.wallet.ValidarTarjetaRequest;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TarjetaMapperImpl implements TarjetaMapper {

    @Override
    public UserTarjeta toUserTarjeta(ValidarTarjetaRequest request, Long userId, String numeroEnmascarado, String holderName) {
        return UserTarjeta.builder()
                .userId(userId)
                .numeroTarjetaCompleto(request.getNumeroTarjeta())
                .numeroTarjetaEnmascarado(numeroEnmascarado)
                .holderName(holderName)
                .verificada(true)
                .activa(true)
                .fechaVerificacion(Instant.now())
                .build();
    }

    @Override
    public ValidarTarjetaResponse toValidarTarjetaResponse(MockTarjeta mockTarjeta, boolean success, String mensaje) {
        return ValidarTarjetaResponse.builder()
                .success(success)
                .mensaje(mensaje)
                .numeroTarjetaEnmascarado(success ? enmascararTarjeta(mockTarjeta.getNumeroTarjeta()) : null)
                .holderName(success ? mockTarjeta.getNombreTitular() : null)
                .build();
    }

    @Override
    public ValidarTarjetaResponse toSuccessResponse(UserTarjeta userTarjeta) {
        return ValidarTarjetaResponse.builder()
                .success(true)
                .mensaje("Tarjeta activada exitosamente")
                .numeroTarjetaEnmascarado(userTarjeta.getNumeroTarjetaEnmascarado())
                .holderName(userTarjeta.getHolderName())
                .build();
    }

    @Override
    public ValidarTarjetaResponse toErrorResponse(String mensaje) {
        return ValidarTarjetaResponse.builder()
                .success(false)
                .mensaje(mensaje)
                .build();
    }

    private String enmascararTarjeta(String numeroTarjeta) {
        if (numeroTarjeta == null || numeroTarjeta.length() < 4) {
            return "**** **** **** ****";
        }
        String ultimos4 = numeroTarjeta.substring(numeroTarjeta.length() - 4);
        return "**** **** **** " + ultimos4;
    }
}