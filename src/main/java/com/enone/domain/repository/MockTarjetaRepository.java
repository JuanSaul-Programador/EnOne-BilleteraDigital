package com.enone.domain.repository;

import com.enone.domain.model.MockTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MockTarjetaRepository extends JpaRepository<MockTarjeta, Long> {

    Optional<MockTarjeta> findByNumeroTarjeta(String numeroTarjeta);

    @Query("SELECT t FROM MockTarjeta t WHERE t.numeroTarjeta = :numero AND t.activa = true")
    Optional<MockTarjeta> findByNumeroTarjetaAndActivaTrue(@Param("numero") String numeroTarjeta);

    @Query("SELECT t FROM MockTarjeta t WHERE t.nombreTitular LIKE %:nombre%")
    List<MockTarjeta> findByNombreTitularContaining(@Param("nombre") String nombre);

    @Query("SELECT COUNT(t) FROM MockTarjeta t WHERE t.activa = true")
    long countActiveCards();

    @Query("SELECT SUM(t.saldoDisponible) FROM MockTarjeta t WHERE t.activa = true")
    BigDecimal sumTotalBalance();

    @Query("SELECT t FROM MockTarjeta t WHERE t.saldoDisponible >= :minBalance")
    List<MockTarjeta> findByMinimumBalance(@Param("minBalance") BigDecimal minBalance);

    boolean existsByNumeroTarjeta(String numeroTarjeta);
}