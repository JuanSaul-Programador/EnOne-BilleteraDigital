package com.enone.application.mapper;


import com.enone.domain.model.RegistrationSession;
import com.enone.domain.model.User;
import com.enone.web.dto.auth.RegisterStartRequest;

public interface OnboardingMapper {
    
    RegistrationSession toRegistrationSession(RegisterStartRequest request, String passwordHash);
    
    User toUser(RegistrationSession session);
}