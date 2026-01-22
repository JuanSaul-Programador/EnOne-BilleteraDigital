package com.enone.application.service.impl;


import com.enone.application.service.OnboardingService;
import com.enone.domain.model.*;
import com.enone.domain.repository.*;
import com.enone.exception.ApiException;


import com.enone.util.PasswordUtil;
import com.enone.util.email.EmailService;
import com.enone.util.sms.SmsService;
import com.enone.web.dto.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final MockReniecRepository reniecRepository;
    private final RegistrationSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository profileRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public static final BigDecimal DEFAULT_USER_LIMIT = new BigDecimal("500.00");
    private static final int SESSION_TTL_MIN = 30;
    private static final int RESEND_COOLDOWN_SEC = 60;
    private static final int MAX_RESENDS = 3;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int LOCK_MIN = 15;

    private final SecureRandom random = new SecureRandom();

    private String code6() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String generateWalletNumber() {
        String number;
        int attempts = 0;
        do {
            number = "EN" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 18)
                .toUpperCase();
            attempts++;
        } while (walletRepository.findByWalletNumber(number).isPresent() && attempts < 5);
        
        return number;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim()
            .toUpperCase()
            .replaceAll("Á", "A")
            .replaceAll("É", "E")
            .replaceAll("Í", "I")
            .replaceAll("Ó", "O")
            .replaceAll("Ú", "U")
            .replaceAll("Ü", "U")
            .replaceAll("Ñ", "N")
            .replaceAll("\\s+", " ");
    }

    @Override
    @Transactional
    public Long start(RegisterStartRequest request) {
        log.info("Iniciando registro para email: {}", request.getEmail());
        
        // Validar que no existan usuarios con el mismo email o teléfono
        userRepository.findByUsername(request.getEmail()).ifPresent(u -> {
            throw new ApiException(409, "El correo ya está registrado");
        });
        profileRepository.findByEmail(request.getEmail()).ifPresent(p -> {
            throw new ApiException(409, "El correo ya está registrado");
        });
        profileRepository.findByPhone(request.getPhone()).ifPresent(p -> {
            throw new ApiException(409, "El teléfono ya está registrado");
        });

        var now = Instant.now();
        var existing = sessionRepository.findActiveByEmailOrPhone(request.getEmail(), request.getPhone(), now).orElse(null);
        RegistrationSession session;

        String emailCodePlain = code6(); 
        String emailCodeHash = PasswordUtil.hash(emailCodePlain);
        String phoneCodePlain = code6();

        if (existing != null && !existing.isExpired() 
                && existing.getEmail().equals(request.getEmail()) 
                && existing.getPhone().equals(request.getPhone())) {
            
            // Reutilizar sesión existente
            session = existing;
            session.setPasswordHash(PasswordUtil.hash(request.getPassword()));
            session.setEmailToken(emailCodeHash);
            session.setPhoneCode(phoneCodePlain);
            session.setEmailCodeSentAt(now);
            session.setPhoneCodeSentAt(now);
            session.setEmailResendCount(session.getEmailResendCount() + 1);
            session.setPhoneResendCount(session.getPhoneResendCount() + 1);
            session.setExpiresAt(now.plus(SESSION_TTL_MIN, ChronoUnit.MINUTES));
            
            // Resetear verificaciones
            session.setEmailVerifyAttempts(0);
            session.setPhoneVerifyAttempts(0);
            session.setEmailVerifiedAt(null);
            session.setPhoneVerifiedAt(null);
            session.setStatus(OnboardingStatus.CREATED);
            
            session = sessionRepository.save(session);
            
            sendVerificationEmail(session, emailCodePlain);
            sendVerificationSms(session);
            
            return session.getId();
        }

        // Expirar sesiones activas con el mismo email o teléfono
        sessionRepository.expireActiveByEmailOrPhone(request.getEmail(), request.getPhone());

        // Crear nueva sesión
        session = RegistrationSession.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(PasswordUtil.hash(request.getPassword()))
                .emailToken(emailCodeHash) 
                .phoneCode(phoneCodePlain) 
                .status(OnboardingStatus.CREATED)
                .createdAt(now)
                .expiresAt(now.plus(SESSION_TTL_MIN, ChronoUnit.MINUTES))
                .emailCodeSentAt(now)
                .phoneCodeSentAt(now)
                .emailResendCount(1)
                .phoneResendCount(1)
                .build();

        session = sessionRepository.save(session);

        try {
            sendVerificationEmail(session, emailCodePlain);
            sendVerificationSms(session);
            
            log.info("Registro iniciado exitosamente para sesión ID: {}", session.getId());
            return session.getId();
        } catch (Exception ex) {
            log.error("Falló envío de notificaciones: {}", ex.getMessage());
            throw new ApiException(502, "No se pudo enviar el código de verificación.");
        }
    }

    @Override
    @Transactional
    public void resendEmailCode(SimpleSession request) {
        log.info("Reenviando código de email para sesión: {}", request.getSessionId());
        
        var session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ApiException(400, "Sesión no encontrada"));
        
        guardSession(session);

        var now = Instant.now();

        if (session.getEmailResendCount() >= MAX_RESENDS)
            throw new ApiException(429, "Límite de reenvíos de email alcanzado");
        if (session.getEmailCodeSentAt() != null && now.isBefore(session.getEmailCodeSentAt().plusSeconds(RESEND_COOLDOWN_SEC)))
            throw new ApiException(429, "Espera antes de reenviar el correo");

        String emailCodePlain = code6(); 
        String emailCodeHash = PasswordUtil.hash(emailCodePlain); 
        
        session.setEmailToken(emailCodeHash);
        session.setEmailResendCount(session.getEmailResendCount() + 1);
        session.setEmailCodeSentAt(now);
        
        sessionRepository.save(session);
        
        sendVerificationEmail(session, emailCodePlain);
        
        log.info("Código de email reenviado para sesión: {}", request.getSessionId());
    }

    @Override
    @Transactional
    public void resendPhoneCode(SimpleSession request) {
        log.info("Reenviando código SMS para sesión: {}", request.getSessionId());
        
        var session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new ApiException(400, "Sesión no encontrada"));
        
        guardSession(session);
        
        var now = Instant.now();
        
        if (session.getPhoneResendCount() >= MAX_RESENDS)
            throw new ApiException(429, "Límite de reenvíos SMS alcanzado");
        if (session.getPhoneCodeSentAt() != null && now.isBefore(session.getPhoneCodeSentAt().plusSeconds(RESEND_COOLDOWN_SEC)))
            throw new ApiException(429, "Espera antes de reenviar SMS");

        session.setPhoneCode(code6());
        session.setPhoneCodeSentAt(now);
        session.setPhoneResendCount(session.getPhoneResendCount() + 1);
        
        sessionRepository.save(session);
        
        sendVerificationSms(session);
        
        log.info("Código SMS reenviado para sesión: {}", request.getSessionId());
    }   
 @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Verificando código de email para sesión: {}", request.getSessionId());
        
        var session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ApiException(400, "Sesión no encontrada"));
        
        guardSession(session); 
        
        session.setEmailVerifyAttempts(session.getEmailVerifyAttempts() + 1);
        
        if (session.getEmailVerifyAttempts() > MAX_VERIFY_ATTEMPTS) {
            session.setLockedUntil(Instant.now().plus(LOCK_MIN, ChronoUnit.MINUTES));
            sessionRepository.save(session);
            throw new ApiException(423, "Demasiados intentos. Sesión bloqueada.");
        }
        
        if (!PasswordUtil.verify(request.getCode(), session.getEmailToken())) {
            sessionRepository.save(session); 
            throw new ApiException(400, "Código incorrecto");
        }
        
        session.setEmailVerifiedAt(Instant.now());
        if (session.getStatus() == OnboardingStatus.CREATED)
            session.setStatus(OnboardingStatus.EMAIL_VERIFIED);
        
        sessionRepository.save(session);
        
        log.info("Email verificado exitosamente para sesión: {}", request.getSessionId());
    }

    @Override
    @Transactional
    public void verifyPhone(VerifyPhoneRequest request) {
        log.info("Verificando código SMS para sesión: {}", request.getSessionId());
        
        var session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new ApiException(400, "Sesión no encontrada"));

        guardSession(session);
        
        session.setPhoneVerifyAttempts(session.getPhoneVerifyAttempts() + 1);
        
        if (session.getPhoneVerifyAttempts() > MAX_VERIFY_ATTEMPTS) {
            session.setLockedUntil(Instant.now().plus(LOCK_MIN, ChronoUnit.MINUTES));
            sessionRepository.save(session);
            throw new ApiException(423, "Demasiados intentos de SMS");
        }
        
        if (!request.getCode().equals(session.getPhoneCode())) {
            sessionRepository.save(session);
            throw new ApiException(400, "Código incorrecto");
        }
        
        session.setPhoneVerifiedAt(Instant.now());
        session.setStatus(OnboardingStatus.PHONE_VERIFIED);
        
        sessionRepository.save(session);
        
        log.info("Teléfono verificado exitosamente para sesión: {}", request.getSessionId());
    }

    @Override
    @Transactional
    public Long complete(RegisterCompleteRequest request) {
        log.info("Completando registro para sesión: {}", request.getSessionId());
        
        var session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new ApiException(400, "Sesión no encontrada"));
        
        guardSession(session);

        if (session.getEmailVerifiedAt() == null || session.getPhoneVerifiedAt() == null)
            throw new ApiException(400, "Verifica email y teléfono antes de completar");

        // Validar tipo de documento
        if (!"DNI".equalsIgnoreCase(request.getDocumentType())) {
            throw new ApiException(400, "Solo se permite registro con DNI");
        }

        // Validar DNI
        if (request.getDocumentNumber() == null || request.getDocumentNumber().trim().length() != 8) {
            throw new ApiException(400, "El DNI debe tener 8 dígitos");
        }

        String dni = request.getDocumentNumber().trim();

        // Consultar RENIEC
        MockReniec reniecData = reniecRepository.findByDni(dni)
            .orElseThrow(() -> new ApiException(404, "DNI no encontrado en los registros de RENIEC"));

        log.info("DNI encontrado en RENIEC: {} - {} {}", 
                reniecData.getDni(), reniecData.getNombres(), reniecData.getApellidos());

        // Validar nombres
        String nombresUsuario = normalizeText(request.getFirstName());
        String nombresReniec = normalizeText(reniecData.getNombres());
        
        if (!nombresUsuario.equals(nombresReniec)) {
            log.error("Nombres no coinciden - Usuario: '{}', RENIEC: '{}'", nombresUsuario, nombresReniec);
            throw new ApiException(400, "Los nombres no coinciden con los registrados en RENIEC");
        }

        // Validar apellidos
        String apellidosUsuario = normalizeText(request.getLastName());
        String apellidosReniec = normalizeText(reniecData.getApellidos());
        
        if (!apellidosUsuario.equals(apellidosReniec)) {
            log.error("Apellidos no coinciden - Usuario: '{}', RENIEC: '{}'", apellidosUsuario, apellidosReniec);
            throw new ApiException(400, "Los apellidos no coinciden con los registrados en RENIEC");
        }

        // Validar edad
        LocalDate hoy = LocalDate.now();
        int edad = Period.between(reniecData.getFechaNacimiento(), hoy).getYears();

        if (edad < 18) {
            throw new ApiException(400, "No puedes registrarte porque eres menor de 18 años");
        }

        // Validar que el DNI no esté ya registrado
        if (profileRepository.findByDocumentNumber(dni).isPresent()) {
            throw new ApiException(409, "Este DNI ya está registrado en el sistema");
        }

        // Validar duplicados de email y teléfono
        userRepository.findByUsername(session.getEmail()).ifPresent(u -> {
            throw new ApiException(409, "Correo ya usado");
        });
        profileRepository.findByEmail(session.getEmail()).ifPresent(p -> {
            throw new ApiException(409, "Correo ya usado");
        });
        profileRepository.findByPhone(session.getPhone()).ifPresent(p -> {
            throw new ApiException(409, "Teléfono ya usado");
        });

        // Actualizar sesión con datos validados
        session.setDocumentType(request.getDocumentType());
        session.setDocumentNumber(dni);
        session.setFirstName(reniecData.getNombres()); 
        session.setLastName(reniecData.getApellidos()); 
        session.setGender(request.getGender());
        session.setBirthDate(reniecData.getFechaNacimiento().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        sessionRepository.save(session);

        // Obtener o crear rol CLIENT
        Role clientRole = roleRepository.findByName("CLIENT").orElse(null);
        if (clientRole == null) {
            clientRole = new Role(null, "CLIENT");
            clientRole = roleRepository.save(clientRole);
        }
        
        // Crear usuario
        User user = new User();
        user.setUsername(session.getEmail());
        user.setPassword(session.getPasswordHash());
        user.setEnabled(true);
        user.getRoles().add(clientRole);

        // Crear perfil
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setEmail(session.getEmail());
        profile.setPhone(session.getPhone());
        profile.setFirstName(session.getFirstName());
        profile.setLastName(session.getLastName());
        profile.setDocumentType(session.getDocumentType());
        profile.setDocumentNumber(session.getDocumentNumber());
        profile.setGender(request.getGender());
        profile.setBirthDate(
            session.getBirthDate() != null 
                ? session.getBirthDate().atZone(java.time.ZoneOffset.UTC).toLocalDate() 
                : null
        );
        
        profile.setDailyTransactionLimit(DEFAULT_USER_LIMIT);

        user.setProfile(profile);
        user = userRepository.save(user);
        
        // Crear wallets PEN y USD
        String walletNumberPEN = generateWalletNumber();
        
        Wallet walletPEN = Wallet.builder()
            .userId(user.getId())
            .walletNumber(walletNumberPEN)
            .balance(BigDecimal.ZERO)
            .currency("PEN")
            .status(WalletStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        walletRepository.save(walletPEN);

        String walletNumberUSD = generateWalletNumber();
        
        Wallet walletUSD = Wallet.builder()
            .userId(user.getId())
            .walletNumber(walletNumberUSD)
            .balance(BigDecimal.ZERO)
            .currency("USD")
            .status(WalletStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        walletRepository.save(walletUSD);

        // Marcar sesión como completada
        session.setStatus(OnboardingStatus.COMPLETED);
        sessionRepository.save(session);

        log.info("Registro completado exitosamente para usuario ID: {} ({})", user.getId(), user.getUsername());
        return user.getId();
    }
    
    private void guardSession(RegistrationSession session) {
        if (session.isExpired())
            throw new ApiException(410, "Sesión expirada");
        if (session.getLockedUntil() != null && Instant.now().isBefore(session.getLockedUntil()))
            throw new ApiException(423, "Sesión bloqueada temporalmente");
    }

    private void sendVerificationEmail(RegistrationSession session, String plainTextCode) {
        String formattedCode = plainTextCode.substring(0, 3) + "-" + plainTextCode.substring(3);

        String html = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px; color: #333; border: 1px solid #ddd; border-radius: 12px; max-width: 400px; margin: auto;'>" +
                      "<h2 style='color: #1e40af;'>Tu Código de Verificación de EnOne</h2>" +
                      "<p style='font-size: 16px;'>Ingresa este código en la aplicación para confirmar tu correo:</p>" +
                      "<div style='font-size: 36px; font-weight: bold; letter-spacing: 5px; margin: 25px 0; padding: 15px; background-color: #f3f4f6; border-radius: 8px;'>" +
                      formattedCode +
                      "</div>" +
                      "<p style='color: #888; font-size: 12px;'>Este código expira en " + SESSION_TTL_MIN + " minutos.</p>" +
                      "</div>";
        
        try {
            emailService.send(session.getEmail(), "Tu Código de Verificación - EnOne", html);
        } catch (Exception e) {
            log.error("Falló el envío de email de verificación: {}", e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }

    private void sendVerificationSms(RegistrationSession session) {
        smsService.send(session.getPhone(), "EnOne: tu código es " + session.getPhoneCode());
    }
}