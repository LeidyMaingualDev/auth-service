package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PasswordChangedNotificationDTO es un DTO (Data Transfer Object) que representa la notificación de cambio de contraseña para un usuario autenticado. Contiene los campos necesarios para validar y procesar la notificación de cambio de contraseña.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangedNotificationDTO {

    /** El ID del usuario */
    private Long userId;
    /** El correo electrónico del usuario */
    private String email;
    /** El tipo de evento */
    private String eventType;
    /** El mensaje de notificación */
    private String message;
    /** La fecha y hora del cambio */
    private LocalDateTime changedAt;
}