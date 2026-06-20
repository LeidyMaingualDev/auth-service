package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Obtiene la información de un usuario por su ID.
     * Usado por otros microservicios (ej. ms-planes) para mostrar datos del organizador.
     *
     * @param id ID del usuario a consultar
     * @return información del usuario
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getUserById(
            @PathVariable Long id) {
        return ResponseEntity.ok(profileService.getUserById(id));
    }

    @Value("${internal.api-key}")
    private String internalApiKey;

    /**
     * Obtiene la información de un usuario por su ID (uso interno entre microservicios).
     * Protegido con una llave compartida en vez de JWT, ya que esta llamada
     * no proviene de un usuario navegando sino de otro microservicio.
     *
     * @param id ID del usuario a consultar
     * @param apiKey llave interna enviada en el header X-Internal-Api-Key
     * @return información del usuario
     */
    @GetMapping("/internal/users/{id}")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getUserByIdInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(internalApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error("Llave interna inválida."));
        }

        return ResponseEntity.ok(profileService.getUserById(id));
    }
}