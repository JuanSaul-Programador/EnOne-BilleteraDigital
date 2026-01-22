package com.enone.application.service.impl;


import com.enone.application.mapper.AuthMapper;
import com.enone.application.service.AuthService;
import com.enone.application.service.JwtService;
import com.enone.application.service.WalletService;
import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.domain.model.Wallet;
import com.enone.domain.repository.UserProfileRepository;
import com.enone.domain.repository.UserRepository;
import com.enone.domain.repository.UserTarjetaRepository;
import com.enone.domain.repository.WalletRepository;
import com.enone.exception.ApiException;

import com.enone.util.PasswordUtil;
import com.enone.util.email.EmailService;
import com.enone.util.sms.SmsService;
import com.enone.web.dto.auth.*;
import com.enone.web.dto.user.ConfirmLimitRequest;
import com.enone.web.dto.user.UpdateLimitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTarjetaRepository userTarjetaRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    private final Random random = new Random();
    private static final BigDecimal MAX_USER_LIMIT = new BigDecimal("2000.00");
    public static final BigDecimal DEFAULT_USER_LIMIT = new BigDecimal("500.00");

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login para usuario: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(401, "Credenciales inválidas"));

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            log.warn("Contraseña incorrecta para usuario: {}", request.getUsername());
            throw new ApiException(401, "Credenciales inválidas");
        }

        if (!user.isEnabled()) {
            log.warn("Usuario deshabilitado intentó hacer login: {}", request.getUsername());
            throw new ApiException(401, "Credenciales inválidas");
        }

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRoles().stream().map(r -> r.getName()).toList()
        );

        long expiresAt = System.currentTimeMillis() + 15 * 60 * 1000; // 15 minutos

        log.info("Login exitoso para usuario: {} (ID: {})", user.getUsername(), user.getId());
        return authMapper.toLoginResponse(user, token, expiresAt);
    }

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(999999));
    }

    @Override
    @Transactional
    public void requestDeletionCode(Long userId, PasswordRequest request) {
        log.info("Solicitando código de eliminación para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new ApiException(403, "Tu cuenta está bloqueada. No puedes iniciar el proceso de eliminación. Contacta a soporte.");
        }

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new ApiException(401, "La contraseña es incorrecta");
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        user.setDeletionCode(hashedOtp);
        user.setDeletionCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);

        String userPhone = user.getProfile().getPhone();
        if (userPhone == null || userPhone.isBlank()) {
            throw new ApiException(500, "No se encontró un teléfono para enviar el código.");
        }

        String smsMessage = "Tu código de eliminación para EnOne es: " + otpCode + ". Expira en 10 minutos.";
        smsService.send(userPhone, smsMessage);
        
        log.info("Código de eliminación enviado para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void confirmDeleteAccount(Long userId, DeleteAccountRequest request) {
        log.info("Confirmando eliminación de cuenta para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new ApiException(401, "La contraseña es incorrecta");
        }

        if (user.getDeletionCode() == null || !PasswordUtil.verify(request.getCode(), user.getDeletionCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (user.getDeletionCodeExpiresAt() == null || Instant.now().isAfter(user.getDeletionCodeExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado. Por favor, solicita uno nuevo.");
        }

        Optional<Wallet> walletUsdOpt = walletRepository.findByUserIdAndCurrency(userId, "USD");
        if (walletUsdOpt.isPresent()) {
            Wallet walletUsd = walletUsdOpt.get();
            BigDecimal saldoUsd = walletUsd.getBalance();

            if (saldoUsd != null && saldoUsd.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Liquidando fondos USD: {} para usuario: {}", saldoUsd, userId);
                try {
                    walletService.convert(userId, "USD", "PEN", saldoUsd, "Liquidación de fondos por cierre de cuenta");
                } catch (Exception e) {
                    throw new ApiException(500, "Error al liquidar fondos (conversión): " + e.getMessage());
                }
            }
        }

        Optional<Wallet> walletPenOpt = walletRepository.findByUserIdAndCurrency(userId, "PEN");
        if (walletPenOpt.isPresent()) {
            Wallet walletPen = walletPenOpt.get();
            BigDecimal saldoPen = walletPen.getBalance();

            if (saldoPen != null && saldoPen.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    walletService.withdraw(userId, saldoPen, "Cierre de cuenta y retiro de fondos.");
                } catch (Exception e) {
                    throw new ApiException(500, "Error al liquidar fondos (retiro): " + e.getMessage());
                }
            }
        }

        UserProfile profile = user.getProfile();
        if (profile != null) {
            profile.setEmail("deleted_" + user.getId() + "@deleted.com");
            profile.setPhone(null);
            profile.setDocumentNumber(null);
            userProfileRepository.save(profile);
        }

        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        user.setUsername("deleted_" + user.getId());
        user.setPassword(PasswordUtil.hash(UUID.randomUUID().toString()));
        user.setDeletionCode(null);
        user.setDeletionCodeExpiresAt(null);
        userRepository.save(user);

        userTarjetaRepository.desactivarTarjetasDeUsuario(userId);
        
        log.info("Cuenta eliminada exitosamente para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void requestEmailChange(Long userId, PasswordRequest request) {
        log.info("Solicitando cambio de email para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new ApiException(401, "La contraseña es incorrecta");
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ApiException(404, "Perfil de usuario no encontrado.");
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        profile.setChangeVerificationCode(hashedOtp);
        profile.setChangeVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userProfileRepository.save(profile);

        String userPhone = profile.getPhone();
        if (userPhone == null || userPhone.isBlank()) {
            throw new ApiException(500, "No se encontró un teléfono para enviar el código de verificación.");
        }

        String smsMessage = "Tu código para iniciar el cambio de email en EnOne es: " + otpCode;
        smsService.send(userPhone, smsMessage);
        
        log.info("Código de cambio de email enviado para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void verifyPhoneForEmailChange(Long userId, VerifyCodeRequest request) {
        log.info("Verificando teléfono para cambio de email, usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));
        
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        if (profile.getChangeVerificationCode() == null || 
            !PasswordUtil.verify(request.getCode(), profile.getChangeVerificationCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (profile.getChangeVerificationExpiresAt() == null || 
            Instant.now().isAfter(profile.getChangeVerificationExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado.");
        }

        profile.setChangeVerificationCode(null);
        profile.setChangeVerificationExpiresAt(null);
        userProfileRepository.save(profile);
        
        log.info("Teléfono verificado para cambio de email, usuario: {}", userId);
    }

    @Override
    @Transactional
    public void sendNewEmailCode(Long userId, NewEmailRequest request) {
        log.info("Enviando código al nuevo email para usuario: {}", userId);
        
        String newEmail = request.getNewEmail().trim().toLowerCase();

        Optional<UserProfile> existingProfile = userProfileRepository.findByEmail(newEmail);
        if (existingProfile.isPresent() && !existingProfile.get().getUserId().equals(userId)) {
            throw new ApiException(409, "El nuevo email ya está en uso por otra cuenta.");
        }

        UserProfile profile = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"))
                .getProfile();
        
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        profile.setPendingNewEmail(newEmail);
        profile.setChangeVerificationCode(hashedOtp);
        profile.setChangeVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userProfileRepository.save(profile);

        String htmlBody = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px; color: #333;'>" +
                "<h2>Código de Verificación de EnOne</h2>" +
                "<p>Ingresa este código para confirmar tu nuevo email:</p>" +
                "<div style='font-size: 36px; font-weight: bold; letter-spacing: 5px; margin: 20px; padding: 10px; background-color: #f4f4f4; border-radius: 8px;'>" +
                otpCode +
                "</div>" +
                "<p style='color: #888; font-size: 12px;'>Este código expira en 10 minutos.</p>" +
                "</div>";

        emailService.send(newEmail, "Confirma tu nuevo email en EnOne", htmlBody);
        
        log.info("Código enviado al nuevo email para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void confirmNewEmail(Long userId, VerifyCodeRequest request) {
        log.info("Confirmando nuevo email para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));
        
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        String pendingEmail = profile.getPendingNewEmail();
        if (pendingEmail == null || pendingEmail.isBlank()) {
            throw new ApiException(400, "No hay ningún cambio de email pendiente.");
        }

        if (profile.getChangeVerificationCode() == null || 
            !PasswordUtil.verify(request.getCode(), profile.getChangeVerificationCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (profile.getChangeVerificationExpiresAt() == null || 
            Instant.now().isAfter(profile.getChangeVerificationExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado.");
        }

        // Actualizar email
        profile.setEmail(pendingEmail);
        user.setUsername(pendingEmail);

        // Limpiar campos temporales
        profile.setPendingNewEmail(null);
        profile.setChangeVerificationCode(null);
        profile.setChangeVerificationExpiresAt(null);

        userRepository.save(user);
        userProfileRepository.save(profile);
        
        log.info("Email actualizado exitosamente para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void requestPhoneChange(Long userId, PasswordRequest request) {
        log.info("Solicitando cambio de teléfono para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new ApiException(401, "La contraseña es incorrecta");
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        profile.setChangeVerificationCode(hashedOtp);
        profile.setChangeVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userProfileRepository.save(profile);

        String userEmail = profile.getEmail();
        if (userEmail == null || userEmail.isBlank()) {
            throw new ApiException(500, "No se encontró un email para enviar el código de verificación.");
        }

        String htmlBody = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px; color: #333;'>" +
                "<h2>Código de Verificación de EnOne</h2>" +
                "<p>Ingresa este código para autorizar el cambio de tu número de teléfono:</p>" +
                "<div style='font-size: 36px; font-weight: bold; letter-spacing: 5px; margin: 20px; padding: 10px; background-color: #f4f4f4; border-radius: 8px;'>" +
                otpCode +
                "</div>" +
                "<p style='color: #888; font-size: 12px;'>Este código expira en 10 minutos.</p>" +
                "</div>";

        emailService.send(userEmail, "Autoriza el cambio de teléfono - EnOne", htmlBody);
        
        log.info("Código de cambio de teléfono enviado por email para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void verifyOldEmailForPhoneChange(Long userId, VerifyCodeRequest request) {
        log.info("Verificando email para cambio de teléfono, usuario: {}", userId);
        
        UserProfile profile = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"))
                .getProfile();
        
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        if (profile.getChangeVerificationCode() == null || 
            !PasswordUtil.verify(request.getCode(), profile.getChangeVerificationCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (profile.getChangeVerificationExpiresAt() == null || 
            Instant.now().isAfter(profile.getChangeVerificationExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado.");
        }

        profile.setChangeVerificationCode(null);
        profile.setChangeVerificationExpiresAt(null);
        userProfileRepository.save(profile);
        
        log.info("Email verificado para cambio de teléfono, usuario: {}", userId);
    }

    @Override
    @Transactional
    public void sendNewPhoneCode(Long userId, NewPhoneRequest request) {
        log.info("Enviando código al nuevo teléfono para usuario: {}", userId);
        
        String newPhone = request.getNewPhone().trim();

        if (!newPhone.matches("^\\+[1-9]\\d{1,14}$")) {
            throw new ApiException(400, "Formato de teléfono inválido. Debe ser E.164 (ej: +51987654321)");
        }

        Optional<UserProfile> existingProfile = userProfileRepository.findByPhone(newPhone);
        if (existingProfile.isPresent() && !existingProfile.get().getUserId().equals(userId)) {
            throw new ApiException(409, "El nuevo teléfono ya está en uso por otra cuenta.");
        }

        UserProfile profile = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"))
                .getProfile();
        
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        profile.setPendingNewPhone(newPhone);
        profile.setChangeVerificationCode(hashedOtp);
        profile.setChangeVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userProfileRepository.save(profile);

        String smsMessage = "Tu código de verificación de EnOne es: " + otpCode;
        smsService.send(newPhone, smsMessage);
        
        log.info("Código enviado al nuevo teléfono para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void confirmNewPhone(Long userId, VerifyCodeRequest request) {
        log.info("Confirmando nuevo teléfono para usuario: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));
        
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ApiException(404, "Perfil no encontrado.");
        }

        String pendingPhone = profile.getPendingNewPhone();
        if (pendingPhone == null || pendingPhone.isBlank()) {
            throw new ApiException(400, "No hay ningún cambio de teléfono pendiente.");
        }

        if (profile.getChangeVerificationCode() == null || 
            !PasswordUtil.verify(request.getCode(), profile.getChangeVerificationCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (profile.getChangeVerificationExpiresAt() == null || 
            Instant.now().isAfter(profile.getChangeVerificationExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado.");
        }

        // Actualizar teléfono
        profile.setPhone(pendingPhone);

        // Limpiar campos temporales
        profile.setPendingNewPhone(null);
        profile.setChangeVerificationCode(null);
        profile.setChangeVerificationExpiresAt(null);

        userProfileRepository.save(profile);
        
        log.info("Teléfono actualizado exitosamente para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void requestLimitChange(Long userId, UpdateLimitRequest request) {
        log.info("Solicitando cambio de límite para usuario: {}", userId);
        
        BigDecimal newLimit = request.getNewLimit();

        if (newLimit == null || newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "El nuevo límite debe ser un monto positivo.");
        }
        if (newLimit.compareTo(DEFAULT_USER_LIMIT) < 0) {
            throw new ApiException(400, "El límite mínimo es de S/ " + DEFAULT_USER_LIMIT.toPlainString());
        }
        if (newLimit.compareTo(MAX_USER_LIMIT) > 0) {
            throw new ApiException(400, "El límite no puede exceder los S/ " + MAX_USER_LIMIT.toPlainString());
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil de usuario no encontrado."));

        // Verificar cooldown de 24 horas
        if (profile.getLastLimitChangeAt() != null) {
            Instant now = Instant.now();
            Instant cooldownEnd = profile.getLastLimitChangeAt().plus(24, ChronoUnit.HOURS);

            if (now.isBefore(cooldownEnd)) {
                long hoursLeft = ChronoUnit.HOURS.between(now, cooldownEnd);
                long minutesLeft = ChronoUnit.MINUTES.between(now, cooldownEnd) % 60;

                String timeLeft = hoursLeft > 0
                        ? hoursLeft + " hora(s) y " + minutesLeft + " minuto(s)"
                        : minutesLeft + " minuto(s)";

                throw new ApiException(429,
                        "Debes esperar 24 horas entre cambios de límite. Tiempo restante: " + timeLeft);
            }
        }

        String otpCode = generateOtpCode();
        String hashedOtp = PasswordUtil.hash(otpCode);

        profile.setPendingNewLimit(newLimit);
        profile.setChangeLimitCode(hashedOtp);
        profile.setChangeLimitExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userProfileRepository.save(profile);

        String userPhone = profile.getPhone();
        if (userPhone == null || userPhone.isBlank()) {
            throw new ApiException(500, "No se encontró un teléfono para enviar el código de verificación.");
        }

        String smsMessage = "Tu código para cambiar tu límite en EnOne es: " + otpCode + ". Expira en 10 minutos.";
        smsService.send(userPhone, smsMessage);
        
        log.info("Código de cambio de límite enviado para usuario: {}", userId);
    }

    @Override
    @Transactional
    public void confirmLimitChange(Long userId, ConfirmLimitRequest request) {
        log.info("Confirmando cambio de límite para usuario: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado."));

        BigDecimal pendingLimit = profile.getPendingNewLimit();
        if (pendingLimit == null) {
            throw new ApiException(400, "No hay ningún cambio de límite pendiente.");
        }

        if (profile.getChangeLimitCode() == null || 
            !PasswordUtil.verify(request.getCode(), profile.getChangeLimitCode())) {
            throw new ApiException(401, "El código de verificación es incorrecto.");
        }

        if (profile.getChangeLimitExpiresAt() == null || 
            Instant.now().isAfter(profile.getChangeLimitExpiresAt())) {
            throw new ApiException(400, "El código de verificación ha expirado.");
        }

        // Actualizar límite
        profile.setDailyTransactionLimit(pendingLimit);
        profile.setLastLimitChangeAt(Instant.now());

        // Limpiar campos temporales
        profile.setPendingNewLimit(null);
        profile.setChangeLimitCode(null);
        profile.setChangeLimitExpiresAt(null);

        userProfileRepository.save(profile);
        
        log.info("Límite actualizado exitosamente a {} para usuario: {}", pendingLimit, userId);
    }

    @Override
    @Transactional
    public void checkAndResetDailyVolume(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

        Instant now = Instant.now();
        Instant lastReset = profile.getLastVolumeResetAt();

        // Resetear si han pasado 24 horas
        if (lastReset == null || now.isAfter(lastReset.plus(24, ChronoUnit.HOURS))) {
            profile.setDailyVolumePen(BigDecimal.ZERO);
            profile.setDailyVolumeUsd(BigDecimal.ZERO);
            profile.setLastVolumeResetAt(now);
            userProfileRepository.save(profile);
            
            log.debug("Volumen diario reseteado para usuario: {}", userId);
        }
    }
}