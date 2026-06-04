package com.auth.services;

import com.auth.models.dtos.PasswordChangedNotificationDTO;
import com.auth.models.dtos.ProfileDeletedNotificationDTO;
import com.auth.models.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio para publicar notificaciones relacionadas con el perfil del usuario,
 * como cambios de contraseña y eliminación de perfil. Utiliza el EmailService
 * para enviar correos electrónicos de notificación a los usuarios cuando se
 * producen eventos relacionados con su perfil. Proporciona métodos para
 * publicar eventos de cambio de contraseña y eliminación de perfil, creando
 * DTOs de notificación con la información relevante del usuario y el evento, y
 * manejando cualquier excepción que pueda ocurrir durante el proceso de
 * publicación de la notificación.
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

    /**
     * Publica una notificación de cambio de contraseña para el usuario
     * especificado.
     * 
     * @param user el usuario para quien se publicará la notificación
     */
    public void publishPasswordChanged(User user) {
        PasswordChangedNotificationDTO event = PasswordChangedNotificationDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(PASSWORD_CHANGED)
                .message("La contrasena del usuario fue actualizada correctamente")
                .changedAt(LocalDateTime.now())
                .build();

        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());
            log.info("Evento de cambio de contrasena preparado: {}", event);
        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de contrasena para usuario {}: {}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Publica una notificación de eliminación de perfil para el usuario
     * especificado.
     *
     * @param user el usuario para quien se publicará la notificación
     */
    public void publishProfileDeleted(User user) {
        ProfileDeletedNotificationDTO event = ProfileDeletedNotificationDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .type(PROFILE_DELETED)
                .message("El perfil del usuario fue desactivado correctamente")
                .status("PENDING_EXTERNAL_NOTIFICATION")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            emailService.sendProfileDeletedEmail(user.getEmail(), user.getName());
            log.info("Evento de eliminación lógica de perfil preparado: {}", event);
        } catch (Exception e) {
            log.error("Error al publicar evento de eliminación lógica de perfil para usuario {}: {}",
                    user.getId(), e.getMessage());
        }
    }
}
