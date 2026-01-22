package com.enone.application.service.impl;


import com.enone.application.service.MockBancoService;
import com.enone.domain.model.MockTarjeta;
import com.enone.domain.repository.MockTarjetaRepository;
import com.enone.exception.ApiException;

import com.enone.web.dto.wallet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockBancoServiceImpl implements MockBancoService {

    private final MockTarjetaRepository tarjetaRepository;

    @Override
    @Transactional
    public ValidarTarjetaResponse validarTarjeta(ValidarTarjetaRequest request) {
        log.info("Validando tarjeta terminada en: ****{}", 
                request.getNumeroTarjeta().substring(request.getNumeroTarjeta().length() - 4));
        
        Optional<MockTarjeta> tarjetaOpt = tarjetaRepository.findByNumeroTarjeta(request.getNumeroTarjeta());
        
        if (tarjetaOpt.isEmpty()) {
            return ValidarTarjetaResponse.builder()
                    .success(false)
                    .mensaje("Tarjeta no encontrada")
                    .build();
        }
        
        MockTarjeta tarjeta = tarjetaOpt.get();
        
        // Validar CVV
        if (!tarjeta.getCvv().equals(request.getCvv())) {
            return ValidarTarjetaResponse.builder()
                    .success(false)
                    .mensaje("CVV incorrecto")
                    .build();
        }
        
        // Validar fecha de vencimiento
        if (!tarjeta.getFechaVencimiento().equals(request.getFechaVencimiento())) {
            return ValidarTarjetaResponse.builder()
                    .success(false)
                    .mensaje("Fecha de vencimiento incorrecta")
                    .build();
        }

        // Validar nombre del titular
        String nombreBanco = tarjeta.getNombreTitular().trim().toUpperCase();
        String nombreRequest = request.getNombreTitular().trim().toUpperCase();

        if (!nombreBanco.equals(nombreRequest)) {
            return ValidarTarjetaResponse.builder()
                    .success(false)
                    .mensaje("Nombre del titular no coincide")
                    .build();
        }
        
        // Validar que esté activa
        if (!tarjeta.getActiva()) {
            return ValidarTarjetaResponse.builder()
                    .success(false)
                    .mensaje("Tarjeta desactivada")
                    .build();
        }
        
        String enmascarado = enmascararTarjeta(tarjeta.getNumeroTarjeta());
        
        log.info("Tarjeta validada exitosamente: {}", enmascarado);

        return ValidarTarjetaResponse.builder()
                .success(true)
                .mensaje("Tarjeta válida")
                .numeroTarjetaEnmascarado(enmascarado)
                .holderName(tarjeta.getNombreTitular()) 
                .build();
    }

    @Override
    @Transactional
    public RealizarCobroResponse realizarCobro(RealizarCobroRequest request) {
        log.info("Realizando cobro por monto: {}", request.getMonto());
        
        Optional<MockTarjeta> tarjetaOpt = tarjetaRepository.findByNumeroTarjeta(request.getNumeroTarjeta());
        
        if (tarjetaOpt.isEmpty()) {
            return RealizarCobroResponse.builder()
                    .success(false)
                    .mensaje("Tarjeta no encontrada")
                    .build();
        }
        
        MockTarjeta tarjeta = tarjetaOpt.get();
        
        // Validar saldo suficiente
        if (tarjeta.getSaldoDisponible().compareTo(request.getMonto()) < 0) {
            return RealizarCobroResponse.builder()
                    .success(false)
                    .mensaje("Saldo insuficiente en la tarjeta")
                    .nuevoSaldo(tarjeta.getSaldoDisponible())
                    .build();
        }
        
        // Realizar el cobro
        BigDecimal nuevoSaldo = tarjeta.getSaldoDisponible().subtract(request.getMonto());
        tarjeta.setSaldoDisponible(nuevoSaldo);
        tarjeta.setUpdatedAt(Instant.now());
        tarjetaRepository.save(tarjeta);
        
        String transactionId = "BCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        log.info("Cobro realizado exitosamente - Monto: {}, ID: {}", request.getMonto(), transactionId);
        
        return RealizarCobroResponse.builder()
                .success(true)
                .mensaje("Cobro realizado exitosamente")
                .transactionId(transactionId)
                .nuevoSaldo(nuevoSaldo)
                .build();
    }
    
    @Override
    @Transactional
    public RealizarAbonoResponse acreditarDinero(RealizarAbonoRequest request) {
        log.info("Acreditando dinero por monto: {}", request.getMonto());
        
        Optional<MockTarjeta> tarjetaOpt = tarjetaRepository.findByNumeroTarjeta(request.getNumeroTarjeta());

        if (tarjetaOpt.isEmpty()) {
            throw new ApiException(404, "Tarjeta de destino no encontrada en el banco.");
        }

        MockTarjeta tarjeta = tarjetaOpt.get();
        
        // Realizar el abono
        BigDecimal nuevoSaldo = tarjeta.getSaldoDisponible().add(request.getMonto());
        tarjeta.setSaldoDisponible(nuevoSaldo);
        tarjeta.setUpdatedAt(Instant.now());
        tarjetaRepository.save(tarjeta);

        String transactionId = "BANK_ABN_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("Abono exitoso a tarjeta - Monto: {}, ID: {}", request.getMonto(), transactionId);

        return RealizarAbonoResponse.builder()
                .success(true)
                .mensaje("Abono recibido exitosamente")
                .transactionId(transactionId)
                .nuevoSaldo(nuevoSaldo)
                .build();
    }
    
    private String enmascararTarjeta(String numeroTarjeta) {
        String ultimos4 = numeroTarjeta.substring(numeroTarjeta.length() - 4);
        return "**** **** **** " + ultimos4;
    }
}