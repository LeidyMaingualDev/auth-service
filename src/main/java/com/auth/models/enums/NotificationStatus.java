package com.auth.models.enums;

/**
 * Estados usados para controlar la trazabilidad de una notificacion.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
public enum NotificationStatus {
    /** La notificacion fue guardada en la bandeja interna. */
    STORED,

    /** El correo asociado a la notificacion fue enviado correctamente. */
    EMAIL_SENT,

    /** El correo asociado a la notificacion fallo y puede reintentarse. */
    EMAIL_FAILED
}
