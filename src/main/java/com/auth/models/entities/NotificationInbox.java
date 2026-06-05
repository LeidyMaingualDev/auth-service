package com.auth.models.entities;

import com.auth.models.enums.NotificationStatus;
import com.auth.models.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una notificacion almacenada en la bandeja de entrada
 * del usuario.
 *
 * <p>
 * Las notificaciones de este modulo se guardan como informacion consultable y
 * no como alertas emergentes. El campo {@code silent} permite identificar que no
 * deben generar interrupciones visibles.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Entity
@Table(name = "notification_inbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInbox {

    /** Identificador unico de la notificacion. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del usuario destinatario. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Correo usado para notificaciones externas cuando aplique. */
    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    /** Nombre del destinatario para personalizar mensajes. */
    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    /** Tipo funcional de la notificacion. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private NotificationType type;

    /** Titulo corto mostrado en la bandeja de entrada. */
    @Column(nullable = false, length = 150)
    private String title;

    /** Mensaje descriptivo de la notificacion. */
    @Column(nullable = false, length = 1000)
    private String message;

    /** Indica si el usuario ya leyo la notificacion. */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** Indica que la notificacion no debe mostrarse como alerta emergente. */
    @Column(nullable = false)
    private boolean silent;

    /** Estado de almacenamiento o envio externo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationStatus status;

    /** Numero de intentos fallidos de envio externo. */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    /** Ultimo error registrado durante el envio externo. */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /** Fecha de creacion de la notificacion. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Fecha de ultima actualizacion de la notificacion. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Inicializa valores por defecto antes de persistir la notificacion.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        read = false;
        silent = true;
        if (status == null) {
            status = NotificationStatus.STORED;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    /**
     * Actualiza la fecha de modificacion antes de guardar cambios.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
