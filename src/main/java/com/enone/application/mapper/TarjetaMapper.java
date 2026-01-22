package com.enone.application.mapper;


import com.enone.domain.model.MockTarjeta;
import com.enone.domain.model.UserTarjeta;
import com.enone.web.dto.wallet.ValidarTarjetaRequest;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;

public interface TarjetaMapper {
    
    UserTarjeta toUserTarjeta(ValidarTarjetaRequest request, Long userId, String numeroEnmascarado, String holderName);
    
    ValidarTarjetaResponse toValidarTarjetaResponse(MockTarjeta mockTarjeta, boolean success, String mensaje);
    
    ValidarTarjetaResponse toSuccessResponse(UserTarjeta userTarjeta);
    
    ValidarTarjetaResponse toErrorResponse(String mensaje);
}