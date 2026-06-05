package com.auth.services;

import com.auth.models.entities.NotificationInbox;
import com.auth.models.enums.NotificationStatus;
import com.auth.models.enums.NotificationType;
import com.auth.repositories.NotificationInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio programado para reintentar el envio de correos asociados a
 * notificaciones que quedaron en estado fallido.
 *
 * <p>
 * La notificacion permanece almacenada en la bandeja aunque el correo falle. El
 * reintento solo aplica para eventos que el modulo tecnico exige notificar por
 * correo, como cambio de contrasena y eliminacion de perfil.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailRetryService {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final NotificationInboxRepository inboxRepository;
    private final NotificationInboxService inboxService;
    private final EmailService emailService;

    /**
     * Reintenta periodicamente correos fallidos sin detener la aplicacion si la
     * tabla de notificaciones aun no existe.
     */
    @Scheduled(fixedDelayString = "${app.notification-retry-delay-ms:300000}")
    public void retryFailedEmailNotifications() {
        try {
            List<NotificationInbox> failedNotifications = inboxRepository
                    .findByStatusAndRetryCountLessThan(NotificationStatus.EMAIL_FAILED, MAX_RETRY_ATTEMPTS);

            failedNotifications.forEach(this::retryEmailNotification);
        } catch (Exception e) {
            log.error("No fue posible ejecutar reintentos de notificaciones: {}", e.getMessage());
        }
    }

    private void retryEmailNotification(NotificationInbox notification) {
        try {
            if (NotificationType.PASSWORD_CHANGED.equals(notification.getType())) {
                emailService.sendPasswordChangedProfileEmail(
                        notification.getRecipientEmail(),
                        notification.getRecipientName());
            } else if (NotificationType.PROFILE_DELETED.equals(notification.getType())) {
                emailService.sendProfileDeletedBeforeDeletionEmail(
                        notification.getRecipientEmail(),
                        notification.getRecipientName());
            } else {
                return;
            }

            inboxService.markEmailSent(notification.getId());
        } catch (Exception e) {
            inboxService.markEmailFailed(notification.getId(), e.getMessage());
            log.error("Reintento fallido para notificacion {}: {}", notification.getId(), e.getMessage());
        }
    }
}
