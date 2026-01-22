package com.enone.application.service;


import com.enone.web.dto.auth.*;
import com.enone.web.dto.user.ConfirmLimitRequest;
import com.enone.web.dto.user.UpdateLimitRequest;

public interface AuthService {
    
    LoginResponse login(LoginRequest request);
    void requestDeletionCode(Long userId, PasswordRequest request);
    void confirmDeleteAccount(Long userId, DeleteAccountRequest request);
    void requestEmailChange(Long userId, PasswordRequest request);
    void verifyPhoneForEmailChange(Long userId, VerifyCodeRequest request);
    void sendNewEmailCode(Long userId, NewEmailRequest request);
    void confirmNewEmail(Long userId, VerifyCodeRequest request);
    void requestPhoneChange(Long userId, PasswordRequest request);
    void verifyOldEmailForPhoneChange(Long userId, VerifyCodeRequest request);
    void sendNewPhoneCode(Long userId, NewPhoneRequest request);
    void confirmNewPhone(Long userId, VerifyCodeRequest request);
    void requestLimitChange(Long userId, UpdateLimitRequest request);
    void checkAndResetDailyVolume(Long userId);
    void confirmLimitChange(Long userId, ConfirmLimitRequest request);
}