package com.auth.services;

import com.auth.exceptions.BusinessException;
import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.NotificationSettingsResponseDTO;
import com.auth.models.entities.NotificationPreference;
import com.auth.models.entities.User;
import com.auth.models.enums.NotificationType;
import com.auth.repositories.NotificationPreferenceRepository;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio para administrar la configuracion de notificaciones del perfil.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ProfileNotificationSettingsService {

    private final RoleGuard roleGuard;
    private final NotificationPreferenceRepository repository;
    private final NotificationInboxService inboxService;

    /**
     * Consulta la configuracion actual de notificaciones del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actual
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<NotificationSettingsResponseDTO> getSettings(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        NotificationPreference preference = getOrCreate(user.getId());
        return ApiResponseDTO.ok("Configuracion de notificaciones consultada", map(preference));
    }

    /**
     * Activa las notificaciones para el usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actualizada
     */
    @Transactional
    public ApiResponseDTO<NotificationSettingsResponseDTO> activate(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        NotificationPreference preference = getOrCreate(user.getId());

        preference.setNotificationsEnabled(true);
        preference.setSilentMode(false);
        preference.setUpdatedAt(LocalDateTime.now());

        NotificationPreference saved = repository.save(preference);

        inboxService.createSettingsNotification(
                user,
                NotificationType.NOTIFICATIONS_ACTIVATED,
                "Notificaciones activadas",
                "Tus notificaciones fueron activadas correctamente");

        return ApiResponseDTO.ok("Notificaciones activadas", map(saved));
    }

    /**
     * Desactiva las notificaciones para el usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actualizada
     */
    @Transactional
    public ApiResponseDTO<NotificationSettingsResponseDTO> disable(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        NotificationPreference preference = getOrCreate(user.getId());

        preference.setNotificationsEnabled(false);
        preference.setSilentMode(false);
        preference.setUpdatedAt(LocalDateTime.now());

        NotificationPreference saved = repository.save(preference);
        return ApiResponseDTO.ok("Notificaciones desactivadas", map(saved));
    }

    /**
     * Activa el modo silencioso para el usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actualizada
     */
    @Transactional
    public ApiResponseDTO<NotificationSettingsResponseDTO> silent(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        NotificationPreference preference = getOrCreate(user.getId());

        if (!preference.isNotificationsEnabled()) {
            throw BusinessException.badRequest("Primero debes activar las notificaciones");
        }

        preference.setSilentMode(true);
        preference.setUpdatedAt(LocalDateTime.now());

        NotificationPreference saved = repository.save(preference);

        inboxService.createSettingsNotification(
                user,
                NotificationType.NOTIFICATIONS_SILENCED,
                "Notificaciones silenciadas",
                "Tus notificaciones seguiran llegando a la bandeja sin alertas visibles");

        return ApiResponseDTO.ok("Modo silencioso activado", map(saved));
    }

    private NotificationPreference getOrCreate(Long userId) {
        return repository.findByUserId(userId)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .notificationsEnabled(false)
                        .silentMode(false)
                        .updatedAt(LocalDateTime.now())
                        .build());
    }

    private NotificationSettingsResponseDTO map(NotificationPreference preference) {
        return NotificationSettingsResponseDTO.builder()
                .userId(preference.getUserId())
                .notificationsEnabled(preference.isNotificationsEnabled())
                .silentMode(preference.isSilentMode())
                .timestamp(preference.getUpdatedAt())
                .build();
    }
}