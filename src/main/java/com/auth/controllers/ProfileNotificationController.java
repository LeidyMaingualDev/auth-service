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
 * Controlador REST para la configuracion y consulta de notificaciones del
 * perfil.
 *
 * <p>
 * Expone endpoints para activar notificaciones, silenciarlas, consultar la
 * bandeja interna y marcar notificaciones como leidas.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
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
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actualizada
     */
    @PutMapping("/activate")
    public ResponseEntity<ApiResponseDTO<NotificationSettingsResponseDTO>> activate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(settingsService.activate(authHeader));
    }

    /**
     * Activa el modo silencioso para el usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return configuracion actualizada
     */
    @PutMapping("/silent")
    public ResponseEntity<ApiResponseDTO<NotificationSettingsResponseDTO>> silent(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(settingsService.silent(authHeader));
    }

    /**
     * Consulta la bandeja de notificaciones del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return lista de notificaciones
     */
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponseDTO<List<NotificationInboxResponseDTO>>> inbox(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(ApiResponseDTO.ok("Bandeja consultada", inboxService.getInbox(authHeader)));
    }

    /**
     * Consulta solo las notificaciones no leidas del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return lista de notificaciones no leidas
     */
    @GetMapping("/inbox/unread")
    public ResponseEntity<ApiResponseDTO<List<NotificationInboxResponseDTO>>> unreadInbox(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(ApiResponseDTO.ok("Notificaciones no leidas", inboxService.getUnreadInbox(authHeader)));
    }

    /**
     * Marca una notificacion como leida.
     *
     * @param id         identificador de la notificacion
     * @param authHeader encabezado Authorization con token Bearer
     * @return notificacion actualizada
     */
    @PutMapping("/inbox/{id}/read")
    public ResponseEntity<ApiResponseDTO<NotificationInboxResponseDTO>> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Notificacion marcada como leida", inboxService.markAsRead(id, authHeader)));
    }
}
