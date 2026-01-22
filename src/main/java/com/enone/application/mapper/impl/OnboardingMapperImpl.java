package com.enone.application.mapper.impl;


import com.enone.application.mapper.OnboardingMapper;
import com.enone.domain.model.OnboardingStatus;
import com.enone.domain.model.RegistrationSession;
import com.enone.domain.model.User;
import com.enone.web.dto.auth.RegisterStartRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OnboardingMapperImpl implements OnboardingMapper {

    private static final int SESSION_TTL_MINUTES = 30;

    @Override
    public RegistrationSession toRegistrationSession(RegisterStartRequest request, String passwordHash) {
        Instant now = Instant.now();
        
        return RegistrationSession.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordHash)
                .status(OnboardingStatus.CREATED)
                .createdAt(now)
                .expiresAt(now.plus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES))
                .emailVerifyAttempts(0)
                .emailResendCount(0)
                .phoneVerifyAttempts(0)
                .phoneResendCount(0)
                .build();
    }

    @Override
    public User toUser(RegistrationSession session) {
        return User.builder()
                .username(session.getEmail())
                .password(session.getPasswordHash())
                .enabled(true)
                .build();
    }
}