package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un rol de seguridad asignable a los usuarios del sistema Qvenly.
 *
 * <p>Los roles definen el nivel de acceso y los permisos que tiene un usuario
 * dentro de la plataforma. El sistema maneja actualmente dos roles:</p>
 * <ul>
 *   <li><b>ADMIN</b> — acceso total al sistema: gestiona planes, usuarios y configuración.</li>
 *   <li><b>USER</b> — rol base asignado automáticamente a todo usuario registrado.</li>
 * </ul>
 *
 * <p>La relación entre usuarios y roles es de muchos a muchos y se gestiona
 * a través de la tabla intermedia {@code user_roles}.</p>
 *
 * <p>Mapa de la tabla {@code roles} en la base de datos {@code auth_and_user}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see User
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /**
     * Identificador único del rol generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Nombre del rol. Único y no nulo.
     * Ejemplos: {@code "USER"}, {@code "ADMIN"}.
     * Este valor es el que se incluye como claim {@code role} en el token JWT
     * y es leído por el Gateway para tomar decisiones de autorización.
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Descripción opcional del rol que documenta su propósito y alcance.
     * Ejemplo: {@code "Acceso total al sistema: gestiona planes, usuarios y configuración"}.
     */
    @Column(name = "description")
    private String description;
}