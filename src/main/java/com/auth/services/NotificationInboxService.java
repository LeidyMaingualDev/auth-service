package com.auth.services;

import com.auth.exceptions.BusinessException;
import com.auth.models.dtos.NotificationInboxResponseDTO;
import com.auth.models.entities.NotificationInbox;
import com.auth.models.entities.NotificationPreference;
import com.auth.models.entities.User;
import com.auth.models.enums.NotificationStatus;
import com.auth.models.enums.NotificationType;
import com.auth.repositories.NotificationInboxRepository;
import com.auth.repositories.NotificationPreferenceRepository;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de crear y consultar notificaciones internas en la bandeja
 * de entrada del usuario.
 *
 * <p>
 * Las notificaciones se guardan como registros silenciosos para que el usuario
 * pueda consultarlas sin recibir alertas emergentes dentro del sistema.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class NotificationInboxService {

    private final NotificationInboxRepository inboxRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final RoleGuard roleGuard;

    /**
     * Crea una notificacion de seguridad que debe registrarse aunque el usuario no
     * haya activado notificaciones generales.
     *
     * @param user    usuario destinatario
     * @param type    tipo de notificacion
     * @param title   titulo de la notificacion
     * @param message mensaje de la notificacion
     * @return notificacion almacenada
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationInbox createSecurityNotification(
            User user,
            NotificationType type,
            String title,
            String message) {

        return create(user, type, title, message);
    }

    /**
     * Crea una notificacion por cambios en el perfil solo si el usuario activo las
     * notificaciones.
     *
     * @param user          usuario destinatario
     * @param changedFields campos modificados durante la actualizacion
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProfileUpdatedNotification(User user, List<String> changedFields) {
        if (!notificationsEnabled(user.getId())) {
            return;
        }

        create(
                user,
                NotificationType.PROFILE_UPDATED,
                "Perfil actualizado",
                "Se actualizaron estos campos: " + String.join(", ", changedFields));
    }

    /**
     * Crea una notificacion relacionada con cambios de configuracion.
     *
     * @param user    usuario destinatario
     * @param type    tipo de notificacion
     * @param title   titulo de la notificacion
     * @param message mensaje de la notificacion
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSettingsNotification(
            User user,
            NotificationType type,
            String title,
            String message) {

        create(user, type, title, message);
    }

    /**
     * Consulta la bandeja de entrada completa del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return lista de notificaciones del usuario
     */
    @Transactional(readOnly = true)
    public List<NotificationInboxResponseDTO> getInbox(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        return inboxRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    /**
     * Consulta las notificaciones no leidas del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return lista de notificaciones no leidas
     */
    @Transactional(readOnly = true)
    public List<NotificationInboxResponseDTO> getUnreadInbox(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        return inboxRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    /**
     * Marca como leida una notificacion perteneciente al usuario autenticado.
     *
     * @param notificationId identificador de la notificacion
     * @param authHeader     encabezado Authorization con token Bearer
     * @return notificacion actualizada
     */
    @Transactional
    public NotificationInboxResponseDTO markAsRead(Long notificationId, String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        NotificationInbox notification = inboxRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("Notificacion no encontrada"));

        if (!notification.getUserId().equals(user.getId())) {
            throw new BusinessException("No tienes permisos para leer esta notificacion", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        return map(inboxRepository.save(notification));
    }

    /**
     * Marca una notificacion como enviada por correo correctamente.
     *
     * @param notificationId identificador de la notificacion
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailSent(Long notificationId) {
        inboxRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(NotificationStatus.EMAIL_SENT);
            notification.setLastError(null);
            inboxRepository.save(notification);
        });
    }

    /**
     * Marca una notificacion como fallida y aumenta el contador de reintentos.
     *
     * @param notificationId identificador de la notificacion
     * @param error          error ocurrido durante el envio externo
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailFailed(Long notificationId, String error) {
        inboxRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(NotificationStatus.EMAIL_FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setLastError(error);
            inboxRepository.save(notification);
        });
    }

    private NotificationInbox create(User user, NotificationType type, String title, String message) {
        NotificationInbox notification = NotificationInbox.builder()
                .userId(user.getId())
                .recipientEmail(user.getEmail())
                .recipientName(user.getName())
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .silent(true)
                .status(NotificationStatus.STORED)
                .retryCount(0)
                .build();

        return inboxRepository.save(notification);
    }

    private boolean notificationsEnabled(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreference::isNotificationsEnabled)
                .orElse(false);
    }

    private NotificationInboxResponseDTO map(NotificationInbox notification) {
        return NotificationInboxResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .silent(notification.isSilent())
                .status(notification.getStatus().name())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}