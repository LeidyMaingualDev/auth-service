package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.SessionSecurityResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.services.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ProfileController es responsable de manejar los endpoints relacionados con el perfil del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 * @see ProfileService
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @param authHeader header de autorizacion
     * @return perfil del usuario autenticado
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileService.getAuthenticatedProfile(authHeader));
    }

    /**
     * Obtiene datos reales de sesion y seguridad del usuario autenticado.
     *
     * @param authHeader header de autorizacion
     * @param userAgent header User-Agent de la solicitud
     * @param request solicitud HTTP actual
     * @return datos de sesion y seguridad
     */
    @GetMapping("/profile/session")
    public ResponseEntity<ApiResponseDTO<SessionSecurityResponseDTO>> getSessionSecurity(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        return ResponseEntity.ok(
                profileService.getSessionSecurity(authHeader, userAgent, getClientIp(request)));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",")[0].trim();
    }
}