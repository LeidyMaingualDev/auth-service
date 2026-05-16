package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que representa un rol de seguridad asignable a los usuarios.
 *
 * <p>Los roles definen los permisos y el nivel de acceso dentro del sistema.
 * Ejemplos de valores: {@code "USER"}, {@code "ADMIN"}, {@code "MODERATOR"}.</p>
 *
 * <p>Tabla en base de datos: {@code roles}</p>
 *
 * @author Equipo Qvenly
 * @version Leidy martinez
 * @see User
 */

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /** Identificador único del rol. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre del rol. Único y no nulo (ej. {@code "USER"}, {@code "ADMIN"}). */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /** Descripción opcional del rol para documentar su propósito. */
    @Column(name = "description")
    private String description;
}
