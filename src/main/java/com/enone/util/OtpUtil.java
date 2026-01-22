package com.enone.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder;

    public OtpUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    /**
     * Genera un código OTP de 6 dígitos
     * @return Código OTP de 6 dígitos
     */
    public String generate6() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Genera un código alfanumérico de longitud específica
     * @param length Longitud del código
     * @return Código alfanumérico
     */
    public String generateAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        
        return code.toString();
    }

    /**
     * Genera un código de 8 caracteres alfanuméricos
     * @return Código de 8 caracteres
     */
    public String generate8() {
        return generateAlphanumeric(8);
    }

    /**
     * Hashea un código OTP
     * @param plainCode Código en texto plano
     * @return Código hasheado
     */
    public String hash(String plainCode) {
        return passwordEncoder.encode(plainCode);
    }

    /**
     * Verifica un código OTP
     * @param plainCode Código en texto plano
     * @param hashedCode Código hasheado
     * @return true si coinciden, false en caso contrario
     */
    public boolean verify(String plainCode, String hashedCode) {
        if (plainCode == null || hashedCode == null) {
            return false;
        }
        return passwordEncoder.matches(plainCode, hashedCode);
    }
}