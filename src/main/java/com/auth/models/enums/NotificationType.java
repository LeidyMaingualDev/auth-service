package com.auth.models.enums;

/**
 * Enumeracion de eventos que pueden generar una notificacion dentro del modulo
 * de perfil.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
public enum NotificationType {
    /** Notificacion generada cuando el usuario cambia su contrasena. */
    PASSWORD_CHANGED,

    /** Notificacion generada cuando el usuario actualiza datos de su perfil. */
    PROFILE_UPDATED,

    /** Notificacion generada antes de eliminar el perfil del usuario. */
    PROFILE_DELETED,

    /** Notificacion generada al activar las notificaciones del perfil. */
    NOTIFICATIONS_ACTIVATED,

    /** Notificacion generada al activar el modo silencioso. */
    NOTIFICATIONS_SILENCED
}
