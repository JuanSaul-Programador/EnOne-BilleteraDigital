package com.enone.domain.repository;

import com.enone.domain.model.UserTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTarjetaRepository extends JpaRepository<UserTarjeta, Long> {

    Optional<UserTarjeta> findTopByUserIdAndActivaTrueOrderByUpdatedAtDesc(Long userId);

    // Legacy method - replaced by above for robustness
    Optional<UserTarjeta> findByUserIdAndActivaTrue(Long userId);

    List<UserTarjeta> findByUserId(Long userId);

    Optional<UserTarjeta> findByNumeroTarjetaCompleto(String numeroTarjeta);

    @Modifying
    @Query("UPDATE UserTarjeta t SET t.activa = false WHERE t.userId = :userId")
    void desactivarTarjetasDeUsuario(@Param("userId") Long userId);

    @Query("SELECT t FROM UserTarjeta t WHERE t.userId = :userId AND t.verificada = true ORDER BY t.createdAt DESC")
    List<UserTarjeta> findVerifiedCardsByUser(@Param("userId") Long userId);

    @Query("SELECT t FROM UserTarjeta t WHERE t.userId = :userId AND t.activa = true AND t.verificada = true")
    List<UserTarjeta> findActiveVerifiedCardsByUser(@Param("userId") Long userId);

    boolean existsByNumeroTarjetaCompleto(String numeroTarjeta);

    @Query("SELECT COUNT(t) FROM UserTarjeta t WHERE t.userId = :userId AND t.verificada = true")
    long countVerifiedCardsByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM UserTarjeta t WHERE t.activa = true")
    long countActiveCards();

    @Modifying
    @Query("UPDATE UserTarjeta t SET t.activa = false WHERE t.id = :cardId")
    void deactivateCard(@Param("cardId") Long cardId);

    @Modifying
    @Query("UPDATE UserTarjeta t SET t.verificada = true WHERE t.id = :cardId")
    void verifyCard(@Param("cardId") Long cardId);
}