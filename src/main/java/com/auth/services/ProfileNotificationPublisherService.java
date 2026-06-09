package com.auth.services;

import com.auth.models.dtos.PasswordChangedNotificationDTO;
import com.auth.models.dtos.ProfileDeletedNotificationDTO;
import com.auth.models.entities.NotificationInbox;
import com.auth.models.entities.User;
import com.auth.models.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio para publicar notificaciones relacionadas con el perfil del usuario,
 * como cambios de contrasena y eliminacion de perfil. Utiliza el EmailService
 * para enviar correos electronicos de notificacion a los usuarios cuando se
 * producen eventos relacionados con su perfil. Proporciona metodos para
 * publicar eventos de cambio de contrasena y eliminacion de perfil, creando
 * DTOs de notificacion con la informacion relevante del usuario y el evento, y
 * manejando cualquier excepcion que pueda ocurrir durante el proceso de
 * publicacion de la notificacion.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileNotificationPublisherService {

    private static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String PROFILE_DELETED = "PROFILE_DELETED";

    private final EmailService emailService;
    private final NotificationInboxService inboxService;

    /**
     * Publica una notificacion de cambio de contrasena para el usuario
     * especificado.
     *
     * <p>
     * Registra la notificacion en bandeja, intenta enviar correo y almacena el
     * estado del envio para permitir reintentos posteriores.
     * </p>
     *
     * @param user el usuario para quien se publicara la notificacion
     */
    public void publishPasswordChanged(User user) {
        PasswordChangedNotificationDTO event = PasswordChangedNotificationDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(PASSWORD_CHANGED)
                .message("La contrasena del usuario fue actualizada correctamente")
                .changedAt(LocalDateTime.now())
                .build();

        NotificationInbox notification = null;
        try {
            notification = inboxService.createSecurityNotification(
                    user,
                    NotificationType.PASSWORD_CHANGED,
                    "Contrasena actualizada",
                    "La contrasena de tu cuenta fue actualizada correctamente");
        } catch (Exception e) {
            log.error("Error al registrar notificacion interna de cambio de contrasena para usuario {}: {}",
                    user.getId(), e.getMessage());
        }

        try {
            emailService.sendPasswordChangedProfileEmail(user.getEmail(), user.getName());
            if (notification != null) {
                inboxService.markEmailSent(notification.getId());
            }
            log.info("Evento de cambio de contrasena preparado: {}", event);
        } catch (Exception e) {
            if (notification != null) {
                inboxService.markEmailFailed(notification.getId(), e.getMessage());
            }
            log.error("Error al publicar evento de cambio de contrasena para usuario {}: {}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Publica una notificacion de eliminacion de perfil para el usuario
     * especificado.
     *
     * <p>
     * El correo se intenta enviar antes de la desvinculacion definitiva. Si el
     * envio falla, el estado queda registrado para reintento y el flujo de
     * eliminacion puede continuar.
     * </p>
     *
     * @param user el usuario para quien se publicara la notificacion
     */
    public void publishProfileDeleted(User user) {
        ProfileDeletedNotificationDTO event = ProfileDeletedNotificationDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .type(PROFILE_DELETED)
                .message("El perfil del usuario fue eliminado correctamente")
                .status("PENDING_EXTERNAL_NOTIFICATION")
                .timestamp(LocalDateTime.now())
                .build();

        NotificationInbox notification = null;
        try {
            notification = inboxService.createSecurityNotification(
                    user,
                    NotificationType.PROFILE_DELETED,
                    "Perfil eliminado",
                    "Tu perfil sera eliminado y desvinculado del sistema");
        } catch (Exception e) {
            log.error("Error al registrar notificacion interna de eliminacion de perfil para usuario {}: {}",
                    user.getId(), e.getMessage());
        }

        try {
            emailService.sendProfileDeletedBeforeDeletionEmail(user.getEmail(), user.getName());
            if (notification != null) {
                inboxService.markEmailSent(notification.getId());
            }
            log.info("Evento de eliminacion de perfil preparado: {}", event);
        } catch (Exception e) {
            if (notification != null) {
                inboxService.markEmailFailed(notification.getId(), e.getMessage());
            }
            log.error("Error al publicar evento de eliminacion de perfil para usuario {}: {}",
                    user.getId(), e.getMessage());
        }
    }
}