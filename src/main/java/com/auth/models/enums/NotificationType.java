package com.auth.models.enums;

public enum NotificationType {

    // ── Perfil y seguridad (ya existían) ──────────────────────
    PASSWORD_CHANGED,
    PROFILE_UPDATED,
    PROFILE_DELETED,
    NOTIFICATIONS_ACTIVATED,
    NOTIFICATIONS_SILENCED,

    // ── Eventos ───────────────────────────────────────────────
    /** Invitación recibida a un evento (RF52, RF53) */
    INVITATION_RECEIVED,

    /** El evento fue cancelado (RF42.1) */
    EVENT_CANCELLED,

    /** El evento fue modificado (RF39.1) */
    EVENT_UPDATED,

    /** El rol del miembro fue cambiado (RF59.1) */
    MEMBER_ROLE_CHANGED,

    /** El miembro fue eliminado del evento (RF60.1) */
    MEMBER_REMOVED,

    // ── Actividades ───────────────────────────────────────────
    /** Asignado a una actividad (RF70) */
    ACTIVITY_ASSIGNED,

    /** Una actividad fue cancelada (RF73.1) */
    ACTIVITY_CANCELLED,

    /** Asistencia registrada al escanear QR (RF78.2) */
    ATTENDANCE_RECORDED
}