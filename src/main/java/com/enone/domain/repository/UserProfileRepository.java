package com.enone.domain.repository;

import com.enone.domain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByPhone(String phone);

    Optional<UserProfile> findByDocumentNumber(String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByDocumentNumber(String documentNumber);

    @Modifying
    @Query("UPDATE UserProfile p SET p.email = :newEmail, p.pendingNewEmail = null, " +
            "p.changeVerificationCode = null, p.changeVerificationExpiresAt = null WHERE p.userId = :userId")
    void confirmEmailChange(@Param("userId") Long userId, @Param("newEmail") String newEmail);

    @Modifying
    @Query("UPDATE UserProfile p SET p.phone = :newPhone, p.pendingNewPhone = null, " +
            "p.changeVerificationCode = null, p.changeVerificationExpiresAt = null WHERE p.userId = :userId")
    void confirmPhoneChange(@Param("userId") Long userId, @Param("newPhone") String newPhone);

    @Modifying
    @Query("UPDATE UserProfile p SET p.dailyTransactionLimit = :newLimit, p.pendingNewLimit = null, " +
            "p.changeLimitCode = null, p.changeLimitExpiresAt = null, p.lastLimitChangeAt = :now WHERE p.userId = :userId")
    void confirmLimitChange(@Param("userId") Long userId, @Param("newLimit") BigDecimal newLimit, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserProfile p SET p.dailyVolumePen = :volume WHERE p.userId = :userId")
    void updateDailyVolumePen(@Param("userId") Long userId, @Param("volume") BigDecimal volume);

    @Modifying
    @Query("UPDATE UserProfile p SET p.dailyVolumeUsd = :volume WHERE p.userId = :userId")
    void updateDailyVolumeUsd(@Param("userId") Long userId, @Param("volume") BigDecimal volume);

    @Modifying
    @Query("UPDATE UserProfile p SET p.dailyVolumePen = 0, p.dailyVolumeUsd = 0, p.lastVolumeResetAt = :now WHERE p.userId = :userId")
    void resetDailyVolumes(@Param("userId") Long userId, @Param("now") Instant now);
}