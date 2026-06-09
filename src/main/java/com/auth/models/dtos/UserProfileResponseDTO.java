package com.auth.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con los datos del perfil del usuario autenticado.
 *
 * <p>Incluye datos personales, rol, estado de la cuenta y foto de perfil
 * para usuarios registrados con Google OAuth2.</p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class UserProfileResponseDTO {

    /** Identificador único del usuario. */
    private Long id;

    /** Nombre completo del usuario (nombre + apellido). */
    private String fullName;

    /** Nombre de pila del usuario. */
    private String name;

    /** Apellido del usuario. */
    private String lastName;

    /** Correo electrónico del usuario. */
    private String email;

    /** Rol principal del usuario (ADMIN o USER). */
    private String role;

    /** Lista completa de roles asignados al usuario. */
    private List<String> roles;

    /** Indica si la cuenta está activa. */
    private Boolean active;

    /** Estado legible de la cuenta: ACTIVO o INACTIVO. */
    private String status;

    /** Tipo de documento de identidad (CC, CE, PASSPORT, TI). */
    private String documentType;

    /** Número de documento de identidad. */
    private String documentNumber;

    /** Número de teléfono de contacto. */
    private String phoneNumber;

    /**
     * URL de la foto de perfil.
     * Solo aplica para usuarios registrados con Google OAuth2.
     * Es {@code null} para usuarios locales.
     */
    private String profilePicture;

    /** Proveedor de autenticación: LOCAL o GOOGLE. */
    private String authProvider;

    /** Fecha y hora de creación del perfil. */
    private LocalDateTime createdAt;
}