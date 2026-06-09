package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.ChangePasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordResponseDTO;
import com.auth.models.dtos.DeleteProfileRequestDTO;
import com.auth.models.dtos.DeleteProfileResponseDTO;
import com.auth.models.dtos.UpdateProfileRequestDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.services.ProfileMutationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para las mutaciones del perfil del usuario:
 * actualización de datos, cambio de contraseña, confirmación y eliminación.
 *
 * @author Natali Ramirez
 * @version 2.0
 * @see ProfileMutationService
 */
@RestController
@RequestMapping("/auth/profile")
@RequiredArgsConstructor
public class ProfileMutationController {

    private final ProfileMutationService profileMutationService;

    /**
     * Actualiza los datos personales del perfil del usuario autenticado.
     *
     * @param request datos de actualización del perfil
     * @param email   email del usuario inyectado por el Gateway en X-User-Email
     * @return perfil actualizado
     */
    @PutMapping
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(profileMutationService.updateProfile(request, email));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * @param request datos del cambio de contraseña
     * @param email   email del usuario inyectado por el Gateway en X-User-Email
     * @return confirmación del cambio
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(profileMutationService.changePassword(request, email));
    }

    /**
     * Confirma la contraseña del usuario como paso previo a una operación sensible.
     *
     * @param request datos de confirmación
     * @param email   email del usuario inyectado por el Gateway en X-User-Email
     * @return confirmación con timestamp
     */
    @PostMapping("/confirm-password")
    public ResponseEntity<ApiResponseDTO<ConfirmPasswordResponseDTO>> confirmPassword(
            @Valid @RequestBody ConfirmPasswordRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(profileMutationService.confirmPassword(request, email));
    }

    /**
     * Elimina definitivamente el perfil del usuario tras confirmar su contraseña.
     *
     * @param request solicitud con la contraseña actual del usuario
     * @param email   email del usuario inyectado por el Gateway en X-User-Email
     * @return resultado de la eliminación
     */
    @DeleteMapping
    public ResponseEntity<ApiResponseDTO<DeleteProfileResponseDTO>> deleteProfile(
            @Valid @RequestBody DeleteProfileRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return ResponseEntity.ok(profileMutationService.deleteProfile(request, email));
    }
}