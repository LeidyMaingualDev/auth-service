package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserProfileResponseDTO es un DTO (Data Transfer Object) que representa la respuesta de perfil de usuario para un usuario autenticado. Contiene los campos necesarios para validar y procesar la respuesta de perfil de usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDTO {

    /** El ID del usuario */
    private Long id;
    /** El nombre completo del usuario */

    private String fullName;
    /** El nombre del usuario */

    private String name;
    /** El apellido del usuario */  

    private String lastName;
    /** El correo electrónico del usuario */

    private String email;
    /** El rol del usuario */

    private String role;
    /** Los roles del usuario */

    private List<String> roles;
    /** El estado de actividad del usuario */

    private Boolean active;
    /** El estado del usuario */

    private String status;
    /** El tipo de documento del usuario */

    private String documentType;
    /** El número de documento del usuario */

    private String documentNumber;
    /** El número de teléfono del usuario */

    private String phoneNumber;
    /** La fecha y hora de creación del perfil */

    private LocalDateTime createdAt;
    /** La fecha y hora de la última actualización del perfil */
}
