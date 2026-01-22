package com.enone.domain.repository;

import com.enone.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username AND u.enabled = true")
    Optional<User> findByUsernameAndEnabledTrue(@Param("username") String username);

    @Query("SELECT u FROM User u JOIN u.profile p WHERE p.email = :identifier OR p.phone = :identifier")
    Optional<User> findByEmailOrPhone(@Param("identifier") String identifier);

    @Query("SELECT u FROM User u JOIN u.profile p WHERE p.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN u.profile p WHERE p.documentNumber = :docNumber")
    Optional<User> findByDocumentNumber(@Param("docNumber") String documentNumber);

    @Modifying
    @Query("UPDATE User u SET u.enabled = false, u.deletedAt = :deletedAt WHERE u.id = :userId")
    void disableUser(@Param("userId") Long userId, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE User u SET u.deletionCode = :code, u.deletionCodeExpiresAt = :expiresAt WHERE u.id = :userId")
    void setDeletionCode(@Param("userId") Long userId, @Param("code") String code, @Param("expiresAt") Instant expiresAt);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countAllActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :timestamp")
    long countNewUsersSince(@Param("timestamp") Instant timestamp);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false")
    long countDisabledUsers();
}