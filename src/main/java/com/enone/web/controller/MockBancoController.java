package com.enone.web.controller;

import com.enone.web.dto.common.ApiResponse;
import com.enone.web.dto.wallet.RealizarAbonoRequest;
import com.enone.web.dto.wallet.RealizarAbonoResponse;
import com.enone.web.dto.wallet.RealizarCobroRequest;
import com.enone.web.dto.wallet.RealizarCobroResponse;
import com.enone.web.dto.wallet.ValidarTarjetaRequest;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;
import com.enone.application.service.MockBancoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/mock-banco")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MockBancoController {
    
    private final MockBancoService mockBancoService;
    
    @PostMapping("/validar-tarjeta")
    public ResponseEntity<ApiResponse<ValidarTarjetaResponse>> validarTarjeta(
            @RequestBody @Valid ValidarTarjetaRequest request) {
        log.info("Validando tarjeta terminada en: ****{}", 
                request.getNumeroTarjeta().substring(request.getNumeroTarjeta().length() - 4));
        
        ValidarTarjetaResponse response = mockBancoService.validarTarjeta(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMensaje()));
        }
    }
    
    @PostMapping("/realizar-cobro")
    public ResponseEntity<ApiResponse<RealizarCobroResponse>> realizarCobro(
            @RequestBody @Valid RealizarCobroRequest request) {
        log.info("Realizando cobro por monto: {}", request.getMonto());
        
        RealizarCobroResponse response = mockBancoService.realizarCobro(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMensaje()));
        }
    }
    
    @PostMapping("/realizar-abono")
    public ResponseEntity<ApiResponse<RealizarAbonoResponse>> realizarAbono(
            @RequestBody @Valid RealizarAbonoRequest request) {
        log.info("Realizando abono por monto: {}", request.getMonto());
        
        RealizarAbonoResponse response = mockBancoService.acreditarDinero(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMensaje()));
        }
    }
}