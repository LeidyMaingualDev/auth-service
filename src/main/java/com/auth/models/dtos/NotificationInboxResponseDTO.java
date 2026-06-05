package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para mostrar una notificacion almacenada en la bandeja de
 * entrada del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInboxResponseDTO {

    /** Identificador de la notificacion. */
    private Long id;

    /** Tipo de notificacion generada. */
    private String type;

    /** Titulo visible en la bandeja de entrada. */
    private String title;

    /** Mensaje descriptivo de la notificacion. */
    private String message;

    /** Indica si la notificacion fue leida. */
    private Boolean read;

    /** Indica si la notificacion no debe mostrarse como alerta emergente. */
    private Boolean silent;

    /** Estado de almacenamiento o envio externo. */
    private String status;

    /** Fecha de creacion de la notificacion. */
    private LocalDateTime createdAt;
}
