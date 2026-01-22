package com.enone.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 120, unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 60)
    private String firstName;

    @Column(length = 60)
    private String lastName;

    @Column(length = 20)
    private String documentType;

    @Column(length = 32)
    private String documentNumber;

    @Column(length = 10)
    private String gender;

    private LocalDate birthDate;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Column(name = "pending_new_email", length = 120)
    private String pendingNewEmail;

    @Column(name = "pending_new_phone", length = 20)
    private String pendingNewPhone;

    @Column(name = "change_verification_code", length = 64)
    private String changeVerificationCode;

    @Column(name = "change_verification_expires_at")
    private Instant changeVerificationExpiresAt;

    @Column(name = "daily_transaction_limit", precision = 19, scale = 2)
    private BigDecimal dailyTransactionLimit;

    @Column(name = "daily_volume_pen", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dailyVolumePen = BigDecimal.ZERO;

    @Column(name = "daily_volume_usd", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dailyVolumeUsd = BigDecimal.ZERO;

    @Column(name = "pending_new_limit", precision = 19, scale = 2)
    private BigDecimal pendingNewLimit;

    @Column(name = "change_limit_code", length = 64)
    private String changeLimitCode;

    @Column(name = "change_limit_expires_at")
    private Instant changeLimitExpiresAt;

    @Column(name = "last_limit_change_at")
    private Instant lastLimitChangeAt;

    @Column(name = "last_volume_reset_at")
    private Instant lastVolumeResetAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}