package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserProfileResponseDTO es un DTO que representa la respuesta de perfil de usuario autenticado.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDTO {

    /** El ID del usuario. */
    private Long id;

    /** El nombre completo del usuario. */
    private String fullName;

    /** El nombre del usuario. */
    private String name;

    /** El apellido del usuario. */
    private String lastName;

    /** El correo electronico del usuario. */
    private String email;

    /** El rol principal del usuario. */
    private String role;

    /** Los roles del usuario. */
    private List<String> roles;

    /** El estado de actividad del usuario. */
    private Boolean active;

    /** El estado textual del usuario. */
    private String status;

    /** El tipo de documento del usuario. */
    private String documentType;

    /** El numero de documento del usuario. */
    private String documentNumber;

    /** El numero de telefono del usuario. */
    private String phoneNumber;

    /** La fecha y hora de creacion del perfil. */
    private LocalDateTime createdAt;

    /** Fecha y hora del ultimo inicio de sesion exitoso. */
    private LocalDateTime lastAccess;
}