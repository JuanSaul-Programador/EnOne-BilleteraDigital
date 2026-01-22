package com.enone.web.controller;

import com.enone.web.dto.auth.*;
import com.enone.web.dto.common.ApiResponse;
import com.enone.web.dto.user.ConfirmLimitRequest;
import com.enone.web.dto.user.UpdateLimitRequest;
import com.enone.web.dto.user.UserResponse;
import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.application.mapper.UserMapper;
import com.enone.domain.repository.UserProfileRepository;
import com.enone.domain.repository.UserRepository;
import com.enone.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        log.info("🔑 Token generado para {}: {}", request.getUsername(), response.getToken() != null ? "SÍ" : "NO");
        log.info("📦 Respuesta completa: {}", response);
        
        ApiResponse<LoginResponse> apiResponse = ApiResponse.success(response);
        log.info("📦 ApiResponse final: {}", apiResponse);
        
        return ResponseEntity.ok(apiResponse);
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        log.info("👤 GET /me - Authentication: {}", authentication);
        log.info("👤 GET /me - Principal: {}", authentication != null ? authentication.getName() : "NULL");
        
        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("👤 GET /me - UserId extraído: {}", userId);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            log.info("👤 GET /me - Usuario encontrado: {}", user.getUsername());
            
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
            log.info("👤 GET /me - Profile encontrado: {}", profile != null ? "SÍ" : "NO");
            
            UserResponse userResponse = userMapper.toResponse(user, profile);
            log.info("✅ GET /me - Respuesta exitosa para userId: {}", userId);
            
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (Exception e) {
            log.error("💥 ERROR en GET /me: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/request-deletion-code")
    public ResponseEntity<ApiResponse<String>> requestDeletionCode(
            Authentication authentication,
            @RequestBody @Valid PasswordRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.requestDeletionCode(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de eliminación enviado a tu teléfono."));
    }
    
    @PostMapping("/delete-account")
    public ResponseEntity<ApiResponse<String>> confirmDeleteAccount(
            Authentication authentication,
            @RequestBody @Valid DeleteAccountRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.confirmDeleteAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cuenta eliminada permanentemente."));
    }
    
    @PostMapping("/change-email/request")
    public ResponseEntity<ApiResponse<String>> requestEmailChange(
            Authentication authentication,
            @RequestBody @Valid PasswordRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.requestEmailChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado a tu teléfono."));
    }
    
    @PostMapping("/change-email/verify-phone")
    public ResponseEntity<ApiResponse<String>> verifyPhoneForEmailChange(
            Authentication authentication,
            @RequestBody @Valid VerifyCodeRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.verifyPhoneForEmailChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Teléfono verificado."));
    }
    
    @PostMapping("/change-email/send-new-email")
    public ResponseEntity<ApiResponse<String>> sendNewEmailCode(
            Authentication authentication,
            @RequestBody @Valid NewEmailRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.sendNewEmailCode(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado a tu nuevo email."));
    }
    
    @PostMapping("/change-email/confirm-new-email")
    public ResponseEntity<ApiResponse<String>> confirmNewEmail(
            Authentication authentication,
            @RequestBody @Valid VerifyCodeRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.confirmNewEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Email actualizado exitosamente."));
    }
    
    @PostMapping("/change-phone/request")
    public ResponseEntity<ApiResponse<String>> requestPhoneChange(
            Authentication authentication,
            @RequestBody @Valid PasswordRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.requestPhoneChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado a tu email."));
    }
    
    @PostMapping("/change-phone/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyOldEmailForPhoneChange(
            Authentication authentication,
            @RequestBody @Valid VerifyCodeRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.verifyOldEmailForPhoneChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Email verificado."));
    }
    
    @PostMapping("/change-phone/send-new-phone")
    public ResponseEntity<ApiResponse<String>> sendNewPhoneCode(
            Authentication authentication,
            @RequestBody @Valid NewPhoneRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.sendNewPhoneCode(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado a tu nuevo teléfono."));
    }
    
    @PostMapping("/change-phone/confirm-new-phone")
    public ResponseEntity<ApiResponse<String>> confirmNewPhone(
            Authentication authentication,
            @RequestBody @Valid VerifyCodeRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.confirmNewPhone(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Teléfono actualizado exitosamente."));
    }
    
    @PostMapping("/change-limit/request")
    public ResponseEntity<ApiResponse<String>> requestLimitChange(
            Authentication authentication,
            @RequestBody @Valid UpdateLimitRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.requestLimitChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado a tu teléfono."));
    }
    
    @PostMapping("/change-limit/confirm")
    public ResponseEntity<ApiResponse<String>> confirmLimitChange(
            Authentication authentication,
            @RequestBody @Valid ConfirmLimitRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        authService.confirmLimitChange(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Límite actualizado exitosamente."));
    }
}