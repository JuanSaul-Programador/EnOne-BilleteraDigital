package com.enone.application.service;


import com.enone.web.dto.wallet.*;

public interface MockBancoService {
    
    ValidarTarjetaResponse validarTarjeta(ValidarTarjetaRequest request);
    RealizarCobroResponse realizarCobro(RealizarCobroRequest request);
    RealizarAbonoResponse acreditarDinero(RealizarAbonoRequest request);
}