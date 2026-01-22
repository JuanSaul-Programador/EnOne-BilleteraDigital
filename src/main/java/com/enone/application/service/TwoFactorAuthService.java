package com.enone.application.service;


import com.enone.web.dto.wallet.TwoFactorSetupResponse;

public interface TwoFactorAuthService {
    
    TwoFactorSetupResponse generateSecret(Long userId);
    boolean verifyAndEnable(Long userId, String code);
    boolean verifyCode(Long userId, String code);
    boolean disable(Long userId, String code);
    boolean isEnabled(Long userId);
    String getCurrentCode(Long userId);
}