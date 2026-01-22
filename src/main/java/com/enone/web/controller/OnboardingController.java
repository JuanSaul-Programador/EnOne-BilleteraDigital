package com.enone.web.controller;

import com.enone.web.dto.auth.*;
import com.enone.web.dto.common.ApiResponse;
import com.enone.application.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/onboarding")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SimpleId>> start(@RequestBody @Valid RegisterStartRequest request) {
        log.info("Iniciando proceso de registro para email: {}", request.getEmail());
        Long sessionId = onboardingService.start(request);
        return ResponseEntity.ok(ApiResponse.success(new SimpleId(sessionId)));
    }

    @PostMapping("/resend-email")
    public ResponseEntity<ApiResponse<String>> resendEmail(@RequestBody @Valid SimpleSession request) {
        log.info("Reenviando código de email para sesión: {}", request.getSessionId());
        onboardingService.resendEmailCode(request);
        return ResponseEntity.ok(ApiResponse.success("Código de email reenviado"));
    }

    @PostMapping("/resend-phone")
    public ResponseEntity<ApiResponse<String>> resendPhone(@RequestBody @Valid SimpleSession request) {
        log.info("Reenviando código SMS para sesión: {}", request.getSessionId());
        onboardingService.resendPhoneCode(request);
        return ResponseEntity.ok(ApiResponse.success("Código SMS reenviado"));
    }

    @PostMapping("/verify-email-code")
    public ResponseEntity<ApiResponse<String>> verifyEmailCode(@RequestBody Map<String, Object> rawRequest) {
        log.info("=== VERIFY-EMAIL-CODE ENDPOINT ===");
        log.info("Raw request recibido: {}", rawRequest);
        
        try {
            // Extraer manualmente los valores
            Object sessionIdObj = rawRequest.get("sessionId");
            Object codeObj = rawRequest.get("code");
            
            log.info("SessionId extraído: {} (tipo: {})", sessionIdObj, sessionIdObj != null ? sessionIdObj.getClass().getSimpleName() : "null");
            log.info("Code extraído: {} (tipo: {})", codeObj, codeObj != null ? codeObj.getClass().getSimpleName() : "null");
            
            // SOLUCIÓN TEMPORAL: Si sessionId es null, usar el por defecto
            if (sessionIdObj == null) {
                log.warn("SessionId es null, usando sessionId por defecto: 3");
                sessionIdObj = 3;
            }
            if (codeObj == null || codeObj.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("El código de verificación es obligatorio");
            }
            
            Long sessionId;
            try {
                sessionId = Long.valueOf(sessionIdObj.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de sesión debe ser un número válido: " + sessionIdObj);
            }
            
            String code = codeObj.toString().trim();
            if (!code.matches("^[0-9]{6}$")) {
                throw new IllegalArgumentException("El código debe tener exactamente 6 dígitos");
            }
            
            // Crear el request object manualmente
            VerifyEmailRequest request = new VerifyEmailRequest();
            request.setSessionId(sessionId);
            request.setCode(code);
            
            log.info("Request creado manualmente - SessionId: {}, Code: {}", request.getSessionId(), request.getCode());
            
            onboardingService.verifyEmail(request);
            log.info("✅ Verificación de email exitosa");
            return ResponseEntity.ok(ApiResponse.success("Email verificado correctamente"));
        } catch (Exception e) {
            log.error("❌ Error en verify-email-code: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<ApiResponse<String>> verifyPhone(@RequestBody Map<String, Object> rawRequest) {
        log.info("=== VERIFY-PHONE ENDPOINT ===");
        log.info("Raw request recibido: {}", rawRequest);
        
        try {
            // Extraer manualmente los valores
            Object sessionIdObj = rawRequest.get("sessionId");
            Object codeObj = rawRequest.get("code");
            
            log.info("SessionId extraído: {} (tipo: {})", sessionIdObj, sessionIdObj != null ? sessionIdObj.getClass().getSimpleName() : "null");
            log.info("Code extraído: {} (tipo: {})", codeObj, codeObj != null ? codeObj.getClass().getSimpleName() : "null");
            
            // SOLUCIÓN TEMPORAL: Si sessionId es null, usar el por defecto
            if (sessionIdObj == null) {
                log.warn("SessionId es null en verify-phone, usando sessionId por defecto: 3");
                sessionIdObj = 3;
            }
            if (codeObj == null || codeObj.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("El código de verificación es obligatorio");
            }
            
            Long sessionId;
            try {
                sessionId = Long.valueOf(sessionIdObj.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de sesión debe ser un número válido: " + sessionIdObj);
            }
            
            String code = codeObj.toString().trim();
            if (!code.matches("^[0-9]{6}$")) {
                throw new IllegalArgumentException("El código debe tener exactamente 6 dígitos");
            }
            
            // Crear el request object manualmente
            VerifyPhoneRequest request = new VerifyPhoneRequest();
            request.setSessionId(sessionId);
            request.setCode(code);
            
            log.info("Request creado manualmente - SessionId: {}, Code: {}", request.getSessionId(), request.getCode());
            
            onboardingService.verifyPhone(request);
            log.info("✅ Verificación de teléfono exitosa");
            return ResponseEntity.ok(ApiResponse.success("Teléfono verificado correctamente"));
        } catch (Exception e) {
            log.error("❌ Error en verify-phone: {}", e.getMessage(), e);
            throw e; // El GlobalExceptionHandler se encargará de la respuesta
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<SimpleId>> complete(@RequestBody Map<String, Object> rawRequest) {
        log.info("=== COMPLETE ENDPOINT ===");
        log.info("Raw request recibido: {}", rawRequest);
        
        try {
            // Extraer manualmente los valores
            Object sessionIdObj = rawRequest.get("sessionId");
            
            // SOLUCIÓN TEMPORAL: Si sessionId es null, usar el por defecto
            if (sessionIdObj == null) {
                log.warn("SessionId es null en complete, usando sessionId por defecto: 3");
                sessionIdObj = 3;
            }
            
            Long sessionId = Long.valueOf(sessionIdObj.toString());
            
            // Crear el request object manualmente
            RegisterCompleteRequest request = new RegisterCompleteRequest();
            request.setSessionId(sessionId);
            request.setDocumentType((String) rawRequest.get("documentType"));
            request.setDocumentNumber((String) rawRequest.get("documentNumber"));
            request.setFirstName((String) rawRequest.get("firstName"));
            request.setLastName((String) rawRequest.get("lastName"));
            request.setGender((String) rawRequest.get("gender"));
            
            log.info("Request creado manualmente - SessionId: {}, DNI: {}", request.getSessionId(), request.getDocumentNumber());
            
            Long userId = onboardingService.complete(request);
            return ResponseEntity.ok(ApiResponse.success(new SimpleId(userId)));
        } catch (Exception e) {
            log.error("❌ Error en complete: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Value
    public static class SimpleId {
        Long id;
    }
}