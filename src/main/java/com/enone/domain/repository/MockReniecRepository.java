package com.enone.domain.repository;

import com.enone.domain.model.MockReniec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MockReniecRepository extends JpaRepository<MockReniec, String> {

    Optional<MockReniec> findByDni(String dni);

    @Query("SELECT r FROM MockReniec r WHERE r.nombres LIKE %:nombre% AND r.apellidos LIKE %:apellido%")
    List<MockReniec> findByNombresAndApellidos(@Param("nombre") String nombres, @Param("apellido") String apellidos);

    @Query("SELECT r FROM MockReniec r WHERE r.fechaNacimiento = :fecha")
    List<MockReniec> findByFechaNacimiento(@Param("fecha") LocalDate fechaNacimiento);

    @Query("SELECT r FROM MockReniec r WHERE r.fechaNacimiento BETWEEN :fechaInicio AND :fechaFin")
    List<MockReniec> findByFechaNacimientoBetween(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin);

    @Query("SELECT COUNT(r) FROM MockReniec r")
    long countTotalRecords();

    boolean existsByDni(String dni);

    @Query("SELECT r FROM MockReniec r WHERE UPPER(r.nombres) = UPPER(:nombres) AND UPPER(r.apellidos) = UPPER(:apellidos)")
    List<MockReniec> findByNombresAndApellidosIgnoreCase(@Param("nombres") String nombres, @Param("apellidos") String apellidos);
}
