package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un token de recuperación de contraseña de un solo uso.
 *
 * <p>Se genera como UUID aleatorio cuando el usuario solicita recuperar su contraseña.
 * El token tiene una validez de 30 minutos y se invalida automáticamente tras ser usado
 * o cuando se genera uno nuevo para el mismo usuario.</p>
 *
 * <p>Tabla en base de datos: {@code password_reset_tokens}</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see com.auth.services.AuthService#forgotPassword
 * @see com.auth.services.AuthService#resetPassword
 */

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /** Identificador único del token de recuperación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Valor UUID del token enviado al correo del usuario. Único e indexado. */
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    /** Usuario propietario del token. Carga lazy para evitar N+1 innecesarios. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Fecha y hora de expiración. El token no es válido si {@code now > expiresAt}. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indica si el token ya fue utilizado para restablecer la contraseña.
     * Un token usado no puede volver a emplearse aunque no haya expirado.
     */
    @Column(name = "used", nullable = false)
    private boolean used = false;

    /** Fecha y hora de creación del token. No se actualiza ({@code updatable = false}). */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Hook de JPA que asigna la fecha de creación antes del primer {@code INSERT}.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Determina si el token ha expirado comparando la fecha actual con {@code expiresAt}.
     *
     * @return {@code true} si el token ya expiró; {@code false} si aún es válido
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}