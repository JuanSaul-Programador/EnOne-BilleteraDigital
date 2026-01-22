package com.enone.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_tarjeta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "numero_tarjeta_completo", nullable = false, unique = true, length = 16)
    private String numeroTarjetaCompleto;

    @Column(name = "numero_tarjeta_enmascarado", nullable = false, length = 20)
    private String numeroTarjetaEnmascarado;

    @Column(name = "holder_name", length = 100)
    private String holderName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verificada = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "fecha_verificacion")
    private Instant fechaVerificacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}