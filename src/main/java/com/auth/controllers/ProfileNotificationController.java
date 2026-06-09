package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.NotificationInboxResponseDTO;
import com.auth.models.dtos.NotificationSettingsResponseDTO;
import com.auth.services.NotificationInboxService;
import com.auth.services.ProfileNotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la configuración y consulta de notificaciones del perfil.
 *
 * @author Natali Ramirez
 * @version 2.0
 */
@RestController
@RequestMapping("/auth/profile/notifications")
@RequiredArgsConstructor
public class ProfileNotificationController {

    private final ProfileNotificationSettingsService settingsService;
    private final NotificationInboxService inboxService;

    /**
     * Activa las notificaciones para el usuario autenticado.
     *
     * @param email email del usuario inyectado por el Gateway en X-User-Email
     * @return configuración actualizada
     */
    @PutMapping("/activate")
    public ResponseEntity<ApiResponseDTO<NotificationSettingsResponseDTO>> activate(
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(settingsService.activate(email));
    }

    /**
     * Activa el modo silencioso para el usuario autenticado.
     *
     * @param email email del usuario inyectado por el Gateway en X-User-Email
     * @return configuración actualizada
     */
    @PutMapping("/silent")
    public ResponseEntity<ApiResponseDTO<NotificationSettingsResponseDTO>> silent(
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(settingsService.silent(email));
    }

    /**
     * Consulta la bandeja de notificaciones del usuario autenticado.
     *
     * @param email email del usuario inyectado por el Gateway en X-User-Email
     * @return lista de notificaciones
     */
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponseDTO<List<NotificationInboxResponseDTO>>> inbox(
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(ApiResponseDTO.ok("Bandeja consultada", inboxService.getInbox(email)));
    }

    /**
     * Consulta solo las notificaciones no leídas del usuario autenticado.
     *
     * @param email email del usuario inyectado por el Gateway en X-User-Email
     * @return lista de notificaciones no leídas
     */
    @GetMapping("/inbox/unread")
    public ResponseEntity<ApiResponseDTO<List<NotificationInboxResponseDTO>>> unreadInbox(
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(ApiResponseDTO.ok("Notificaciones no leidas",
                inboxService.getUnreadInbox(email)));
    }

    /**
     * Marca una notificación como leída.
     *
     * @param id    identificador de la notificación
     * @param email email del usuario inyectado por el Gateway en X-User-Email
     * @return notificación actualizada
     */
    @PutMapping("/inbox/{id}/read")
    public ResponseEntity<ApiResponseDTO<NotificationInboxResponseDTO>> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(ApiResponseDTO.ok("Notificacion marcada como leida",
                inboxService.markAsRead(id, email)));
    }
}