package com.enone.application.service.impl;


import com.enone.application.service.TwoFactorAuthService;
import com.enone.domain.model.UserProfile;
import com.enone.domain.repository.UserProfileRepository;
import com.enone.exception.ApiException;

import com.enone.web.dto.wallet.TwoFactorSetupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private final UserProfileRepository userProfileRepository;
    private final SecureRandom random = new SecureRandom();

    private static final long CODE_VALIDITY_MINUTES = 5;

    @Override
    @Transactional
    public TwoFactorSetupResponse generateSecret(Long userId) {
        log.info("Generando código 2FA para usuario: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

        // Generar código numérico de 6 dígitos
        String setupCode = String.format("%06d", random.nextInt(1000000));

        // Almacenar código con timestamp
        String codeWithTimestamp = setupCode + "|" + Instant.now().toEpochMilli();

        profile.setTwoFactorSecret(codeWithTimestamp);
        userProfileRepository.save(profile);

        log.info("Código numérico generado para userId {}: {} (válido por {} minutos)", 
                userId, setupCode, CODE_VALIDITY_MINUTES);

        return TwoFactorSetupResponse.builder()
                .secret(setupCode)
                .qrCode(null)
                .manualEntry(setupCode)
                .build();
    }

    @Override
    @Transactional
    public boolean verifyAndEnable(Long userId, String code) {
        log.info("Verificando y habilitando 2FA para usuario: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

        String storedData = profile.getTwoFactorSecret();
        if (storedData == null || storedData.isEmpty()) {
            throw new ApiException(400, "No se ha generado un código 2FA. Genera uno primero.");
        }

        // Parsear código y timestamp
        String[] parts = storedData.split("\\|");
        if (parts.length != 2) {
            throw new ApiException(400, "Código 2FA inválido. Genera uno nuevo.");
        }

        String setupCode = parts[0];
        long timestamp = Long.parseLong(parts[1]);

        // Verificar expiración
        Instant codeTime = Instant.ofEpochMilli(timestamp);
        Instant now = Instant.now();
        long minutesElapsed = ChronoUnit.MINUTES.between(codeTime, now);

        if (minutesElapsed > CODE_VALIDITY_MINUTES) {
            throw new ApiException(400, "El código ha expirado. Genera uno nuevo.");
        }

        log.debug("Verificando código - Esperado: {}, Recibido: {}, Tiempo restante: {} minutos", 
                setupCode, code, (CODE_VALIDITY_MINUTES - minutesElapsed));

        boolean isValid = setupCode.equals(code.trim());

        if (isValid) {
            // Generar secreto permanente y habilitar 2FA
            String permanentSecret = generatePermanentSecret();

            profile.setTwoFactorEnabled(true);
            profile.setTwoFactorSecret(permanentSecret);
            userProfileRepository.save(profile);

            log.info("2FA activado exitosamente para usuario: {}", userId);
        } else {
            log.warn("Código incorrecto para usuario: {}", userId);
        }

        return isValid;
    }

    @Override
    public boolean verifyCode(Long userId, String code) {
        log.debug("Verificando código 2FA para usuario: {}", userId);

        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

            if (!profile.getTwoFactorEnabled() || profile.getTwoFactorSecret() == null) {
                log.debug("2FA no está activado para usuario: {}", userId);
                return false;
            }

            String storedSecret = profile.getTwoFactorSecret();
            String currentCode = generateCurrentCode(storedSecret);

            log.debug("Verificando código - Esperado: {}, Recibido: {}", currentCode, code);

            boolean isValid = currentCode.equals(code.trim());
            log.debug("Código válido: {}", isValid);

            return isValid;

        } catch (Exception e) {
            log.error("Error al verificar código 2FA para usuario {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean disable(Long userId, String code) {
        log.info("Desactivando 2FA para usuario: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

        if (!profile.getTwoFactorEnabled()) {
            throw new ApiException(400, "2FA no está activado");
        }

        boolean isValid = verifyCode(userId, code);

        if (isValid) {
            profile.setTwoFactorEnabled(false);
            profile.setTwoFactorSecret(null);
            userProfileRepository.save(profile);

            log.info("2FA desactivado exitosamente para usuario: {}", userId);
        } else {
            log.warn("Código incorrecto al intentar desactivar 2FA para usuario: {}", userId);
        }

        return isValid;
    }

    @Override
    public boolean isEnabled(Long userId) {
        try {
            return userProfileRepository.findByUserId(userId)
                    .map(UserProfile::getTwoFactorEnabled)
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error al verificar estado 2FA para usuario {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public String getCurrentCode(Long userId) {
        log.debug("Generando código actual para usuario: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(404, "Perfil no encontrado"));

        if (!profile.getTwoFactorEnabled() || profile.getTwoFactorSecret() == null) {
            throw new ApiException(400, "2FA no está activado");
        }

        return generateCurrentCode(profile.getTwoFactorSecret());
    }

    private String generatePermanentSecret() {
        // Generar secreto hexadecimal de 32 bytes
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateCurrentCode(String secret) {
        // Generar código basado en ventana de tiempo de 5 minutos (300 segundos)
        long timeWindow = System.currentTimeMillis() / 1000 / 300;

        // Generar código de 6 dígitos basado en el secreto y la ventana de tiempo
        int hashCode = (secret + timeWindow).hashCode();
        int code = Math.abs(hashCode) % 1000000;

        return String.format("%06d", code);
    }
}