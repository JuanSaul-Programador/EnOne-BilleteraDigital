package com.enone.application.mapper.impl;


import com.enone.application.mapper.UserMapper;
import com.enone.domain.model.Role;
import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.web.dto.user.UserResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class UserMapperImpl implements UserMapper {

    private static final BigDecimal USD_TO_PEN_RATE = new BigDecimal("3.75");

    @Override
    public UserResponse toResponse(User user, UserProfile profile) {
        BigDecimal dailyVolumePen = profile != null ? profile.getDailyVolumePen() : BigDecimal.ZERO;
        BigDecimal dailyVolumeUsd = profile != null ? profile.getDailyVolumeUsd() : BigDecimal.ZERO;
        

        BigDecimal totalVolumeInPen = dailyVolumePen.add(
            dailyVolumeUsd.multiply(USD_TO_PEN_RATE)
        );
        
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()))
                .email(profile != null ? profile.getEmail() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .firstName(profile != null ? profile.getFirstName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .documentType(profile != null ? profile.getDocumentType() : null)
                .documentNumber(profile != null ? profile.getDocumentNumber() : null)
                .gender(profile != null ? profile.getGender() : null)
                .birthDate(profile != null ? profile.getBirthDate() : null)
                .createdAt(profile != null ? profile.getCreatedAt() : null)
                .dailyTransactionLimit(profile != null ? profile.getDailyTransactionLimit() : null)
                .dailyVolumePen(dailyVolumePen)
                .dailyVolumeUsd(dailyVolumeUsd)
                .totalDailyVolumeInPen(totalVolumeInPen)
                .twoFactorEnabled(profile != null ? profile.getTwoFactorEnabled() : false)
                .build();
    }

    @Override
    public UserResponse toResponse(User user) {
        return toResponse(user, user.getProfile());
    }
}