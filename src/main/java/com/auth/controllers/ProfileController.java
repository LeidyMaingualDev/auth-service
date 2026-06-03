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

    /**
     * The ProfileService es responsable de manejar la lógica de negocio relacionada con los perfiles de usuario.
     */
    private final ProfileService profileService;

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @param authHeader el header de autorización que contiene el token JWT del usuario autenticado
     * @return la respuesta con el perfil del usuario autenticado o un error en caso de que la autenticación falle.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileService.getAuthenticatedProfile(authHeader));
    }
}
