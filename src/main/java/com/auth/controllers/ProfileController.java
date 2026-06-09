package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consultar el perfil del usuario autenticado.
 *
 * @author Natali Ramirez
 * @version 2.0
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
     * @param email email del usuario inyectado por el Gateway en el header X-User-Email
     * @return perfil del usuario autenticado
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getProfile(
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(profileService.getAuthenticatedProfile(email));
    }
}