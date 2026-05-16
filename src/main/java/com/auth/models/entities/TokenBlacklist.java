package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que almacena tokens JWT revocados (blacklist).
 *
 * <p>Cuando un usuario cierra sesión, su token activo se agrega a esta tabla.
 * El filtro {@code JwtAuthFilter} consulta esta tabla en cada petición para
 * rechazar tokens que, aunque técnicamente válidos y no expirados, han sido
 * invalidados explícitamente por el usuario.</p>
 *
 * <p>El campo {@code token} tiene longitud 500 para acomodar tokens JWT
 * con claims adicionales (rol, rememberMe) que pueden ser más largos.</p>
 *
 * <p>Tabla en base de datos: {@code token_blacklist}</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see com.auth.security.JwtAuthFilter
 * @see com.auth.services.AuthService#logout
 */

@Entity
@Table(name = "token_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    /** Identificador único del registro en la blacklist. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador único del registro en la blacklist. */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /** Fecha y hora en que el token fue invalidado. Asignada en {@link #onCreate()}. */
    @Column(name = "invalidated_at", nullable = false)
    private LocalDateTime invalidatedAt;

    /**
     * Hook de JPA que registra la marca de tiempo antes del primer {@code INSERT}.
     */
    @PrePersist
    protected void onCreate() {
        this.invalidatedAt = LocalDateTime.now();
    }
}