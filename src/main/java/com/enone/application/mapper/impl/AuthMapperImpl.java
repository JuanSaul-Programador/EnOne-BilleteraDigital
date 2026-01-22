package com.enone.application.mapper.impl;


import com.enone.application.mapper.AuthMapper;
import com.enone.domain.model.Role;
import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.web.dto.auth.LoginResponse;
import com.enone.web.dto.auth.RegisterCompleteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class AuthMapperImpl implements AuthMapper {

    private static final BigDecimal DEFAULT_USER_LIMIT = new BigDecimal("500.00");

    @Override
    public LoginResponse toLoginResponse(User user, String token, long expiresAt) {
        return LoginResponse.builder()
                .success(true)
                .message("Login exitoso")
                .token(token)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()))
                .build();
    }

    @Override
    public UserProfile toUserProfile(RegisterCompleteRequest request, Long userId) {
        return UserProfile.builder()
                .userId(userId)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dailyTransactionLimit(DEFAULT_USER_LIMIT)
                .dailyVolumePen(BigDecimal.ZERO)
                .dailyVolumeUsd(BigDecimal.ZERO)
                .twoFactorEnabled(false)
                .build();
    }
}