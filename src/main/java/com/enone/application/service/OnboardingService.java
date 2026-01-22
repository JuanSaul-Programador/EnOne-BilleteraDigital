package com.enone.application.service;


import com.enone.web.dto.auth.*;

public interface OnboardingService {
    
    Long start(RegisterStartRequest request);
    void resendEmailCode(SimpleSession request);
    void resendPhoneCode(SimpleSession request);
    void verifyEmail(VerifyEmailRequest request);
    void verifyPhone(VerifyPhoneRequest request);
    Long complete(RegisterCompleteRequest request);
}