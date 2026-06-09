package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProfileDeletedNotificationDTO es un DTO (Data Transfer Object) que representa la notificación de eliminación de perfil para un usuario autenticado. Contiene los campos necesarios para validar y procesar la notificación de eliminación de perfil.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDeletedNotificationDTO {

    /** El ID del usuario */
    private Long userId;

    /** El correo electrónico del usuario */
    private String email;

    /** El tipo de evento */
    private String type;

    /** El mensaje de notificación */
    private String message;

    /** El estado de la notificación */
    private String status;

    /** La fecha y hora de la notificación */
    private LocalDateTime timestamp;
}