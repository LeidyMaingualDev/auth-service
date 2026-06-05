package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la configuracion de notificaciones del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponseDTO {

    /** Identificador del usuario propietario de la configuracion. */
    private Long userId;

    /** Indica si las notificaciones estan activadas. */
    private Boolean notificationsEnabled;

    /** Indica si el usuario activo el modo silencioso. */
    private Boolean silentMode;

    /** Fecha de actualizacion de la configuracion. */
    private LocalDateTime timestamp;
}
