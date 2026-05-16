package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que registra cada intento de inicio de sesión en el sistema.
 *
 * <p>Permite auditar la actividad de autenticación por usuario e IP, y detectar
 * ataques de fuerza bruta contando los intentos fallidos recientes. Cuando el número
 * de intentos fallidos supera el umbral configurado ({@code app.max-login-attempts}),
 * se envía una alerta de seguridad al correo del usuario.</p>
 *
 * <p>Tabla en base de datos: {@code login_attempts}</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see com.auth.services.AuthService
 */

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    /** Identificador único del intento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico con el que se intentó iniciar sesión. */
    @Column(name = "email", nullable = false)
    private String email;

    /** Dirección IP desde la que se realizó el intento. Puede ser {@code null} si no se pudo obtener. */
    @Column(name = "ip_address")
    private String ipAddress;

    /** {@code true} si el intento fue exitoso; {@code false} si las credenciales fueron incorrectas. */
    @Column(name = "success", nullable = false)
    private boolean success;

    /** Fecha y hora exacta del intento. Asignada automáticamente en {@link #onCreate()}. */
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    /**
     * Hook de JPA que registra la marca de tiempo antes del primer {@code INSERT}.
     */
    @PrePersist
    protected void onCreate() {
        this.attemptedAt = LocalDateTime.now();
    }
}