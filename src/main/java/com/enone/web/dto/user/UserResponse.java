package com.enone.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private Boolean enabled;
    private List<String> roles;
    

    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String gender;
    private LocalDate birthDate;
    private Instant createdAt;
    

    private BigDecimal dailyTransactionLimit;
    private BigDecimal dailyVolumePen;
    private BigDecimal dailyVolumeUsd;
    private BigDecimal totalDailyVolumeInPen;
    

    private Boolean twoFactorEnabled;
}