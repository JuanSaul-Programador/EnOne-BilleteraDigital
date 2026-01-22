package com.enone.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "registration_session",
        indexes = {
                @Index(name = "idx_session_email", columnList = "email"),
                @Index(name = "idx_session_phone", columnList = "phone")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 64)
    private String emailToken;

    private Instant emailCodeSentAt;

    @Builder.Default
    private Integer emailResendCount = 0;

    @Column(length = 6)
    private String phoneCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(name = "email_verify_attempts")
    @Builder.Default
    private Integer emailVerifyAttempts = 0;

    private Instant phoneCodeSentAt;

    @Builder.Default
    private Integer phoneResendCount = 0;

    @Builder.Default
    private Integer phoneVerifyAttempts = 0;

    private Instant lockedUntil;

    private Instant emailVerifiedAt;
    private Instant phoneVerifiedAt;

    @Column(length = 20)
    private String documentType;

    @Column(length = 32)
    private String documentNumber;

    @Column(length = 60)
    private String firstName;

    @Column(length = 60)
    private String lastName;

    @Column(length = 10)
    private String gender;

    private Instant birthDate;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt) || status == OnboardingStatus.EXPIRED;
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
}