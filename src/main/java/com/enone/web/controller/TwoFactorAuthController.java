package com.enone.web.controller;

import com.enone.web.dto.common.ApiResponse;
import com.enone.web.dto.wallet.TwoFactorSetupResponse;
import com.enone.web.dto.wallet.VerifyTwoFactorRequest;
import com.enone.application.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> generateSecret(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        log.info("Generando código 2FA para userId: {}", userId);
        
        try {
            TwoFactorSetupResponse response = twoFactorAuthService.generateSecret(userId);
            log.info("✅ Código 2FA generado exitosamente para userId: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("❌ Error al generar código 2FA: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyAndEnable(
            Authentication authentication,
            @RequestBody @Valid VerifyTwoFactorRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        
        log.info("Verificando código 2FA para userId: {}", userId);
        
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Código requerido"));
        }
        
        try {
            boolean valid = twoFactorAuthService.verifyAndEnable(userId, request.getCode());
            
            if (valid) {
                log.info("✅ 2FA activado exitosamente para userId: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("2FA activado exitosamente"));
            } else {
                log.warn("❌ Código incorrecto para userId: {}", userId);
                return ResponseEntity.badRequest().body(ApiResponse.error("Código incorrecto o expirado"));
            }
            
        } catch (Exception e) {
            log.error("❌ Error al verificar 2FA: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<String>> disable(
            Authentication authentication,
            @RequestBody @Valid VerifyTwoFactorRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        
        log.info("Desactivando 2FA para userId: {}", userId);
        
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Código requerido"));
        }
        
        try {
            boolean valid = twoFactorAuthService.disable(userId, request.getCode());
            
            if (valid) {
                log.info("✅ 2FA desactivado exitosamente para userId: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("2FA desactivado exitosamente"));
            } else {
                log.warn("❌ Código incorrecto para desactivar 2FA, userId: {}", userId);
                return ResponseEntity.badRequest().body(ApiResponse.error("Código incorrecto"));
            }
            
        } catch (Exception e) {
            log.error("❌ Error al desactivar 2FA: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getStatus(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        log.debug("Consultando estado 2FA para userId: {}", userId);
        
        boolean enabled = twoFactorAuthService.isEnabled(userId);
        
        log.debug("Estado 2FA para userId {}: {}", userId, enabled ? "Activado" : "Desactivado");
        
        return ResponseEntity.ok(ApiResponse.success(Map.of("enabled", enabled)));
    }

    @GetMapping("/current-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentCode(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        log.info("Obteniendo código actual 2FA para userId: {}", userId);
        
        try {
            String currentCode = twoFactorAuthService.getCurrentCode(userId);
            
            log.info("✅ Código actual 2FA generado para userId: {}", userId);
            
            Map<String, Object> response = Map.of(
                "code", currentCode,
                "expiresIn", 300 // 5 minutos
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("❌ Error al obtener código actual 2FA: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}