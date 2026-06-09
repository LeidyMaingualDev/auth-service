package com.auth.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que almacena la configuracion de notificaciones del usuario.
 *
 * <p>
 * Permite saber si el usuario activo las notificaciones y si desea recibirlas
 * en modo silencioso, manteniendolas en bandeja sin alertas visibles.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    /** Identificador unico de la preferencia. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del usuario propietario de la configuracion. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Indica si el usuario activo las notificaciones. */
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    /** Indica si las notificaciones deben ser silenciosas. */
    @Column(name = "silent_mode", nullable = false)
    private boolean silentMode;

    /** Fecha de ultima modificacion de la configuracion. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}