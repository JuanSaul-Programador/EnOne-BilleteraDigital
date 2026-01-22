package com.enone.domain.repository;

import com.enone.domain.model.OnboardingStatus;
import com.enone.domain.model.RegistrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationSessionRepository extends JpaRepository<RegistrationSession, Long> {

    @Query("SELECT s FROM RegistrationSession s WHERE (s.email = :email OR s.phone = :phone) " +
            "AND s.status NOT IN ('COMPLETED', 'EXPIRED') AND s.expiresAt > :now")
    Optional<RegistrationSession> findActiveByEmailOrPhone(
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("now") Instant now
    );

    @Modifying
    @Query("UPDATE RegistrationSession s SET s.status = 'EXPIRED' WHERE (s.email = :email OR s.phone = :phone) " +
            "AND s.status NOT IN ('COMPLETED', 'EXPIRED')")
    void expireActiveByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);

    @Query("SELECT s FROM RegistrationSession s WHERE s.status = :status")
    List<RegistrationSession> findByStatus(@Param("status") OnboardingStatus status);

    @Query("SELECT s FROM RegistrationSession s WHERE s.expiresAt < :now AND s.status NOT IN ('COMPLETED', 'EXPIRED')")
    List<RegistrationSession> findExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE RegistrationSession s SET s.status = 'EXPIRED' WHERE s.expiresAt < :now AND s.status NOT IN ('COMPLETED', 'EXPIRED')")
    void expireOldSessions(@Param("now") Instant now);

    @Query("SELECT COUNT(s) FROM RegistrationSession s WHERE s.status = :status")
    long countByStatus(@Param("status") OnboardingStatus status);

    @Query("SELECT s FROM RegistrationSession s WHERE s.email = :email ORDER BY s.createdAt DESC")
    List<RegistrationSession> findByEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT s FROM RegistrationSession s WHERE s.phone = :phone ORDER BY s.createdAt DESC")
    List<RegistrationSession> findByPhoneOrderByCreatedAtDesc(@Param("phone") String phone);

    @Modifying
    @Query("DELETE FROM RegistrationSession s WHERE s.status = 'EXPIRED' AND s.createdAt < :cutoffDate")
    void deleteExpiredSessionsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT s FROM RegistrationSession s WHERE s.lockedUntil IS NOT NULL AND s.lockedUntil > :now")
    List<RegistrationSession> findLockedSessions(@Param("now") Instant now);
}