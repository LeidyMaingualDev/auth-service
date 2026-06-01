package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.ChangePasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordResponseDTO;
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
 * ProfileMutationController es responsable de manejar los endpoints relacionados con las mutaciones(edición) del perfil del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 * @see ProfileMutationService
 */
@RestController
@RequestMapping("/auth/profile")
@RequiredArgsConstructor
public class ProfileMutationController {

    /**
     * The ProfileMutationService es responsable de manejar la lógica de negocio relacionada con las mutaciones del perfil de usuario.
     */
    private final ProfileMutationService profileMutationService;

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * @param request the update profile request
     * @param authHeader the authorization header
     * @return la respuesta con el perfil actualizado o un error en caso de que la actualización falle.
     */
    @PutMapping
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileMutationService.updateProfile(request, authHeader));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * @param request the change password request
     * @param authHeader the authorization header
     * @return la respuesta indicando el resultado de la operación (contraseña editada exitosamente o error)
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileMutationService.changePassword(request, authHeader));
    }

    /**
     * Confirma la contraseña del usuario autenticado.
     *
     * @param request the confirm password request
     * @param authHeader the authorization header
     * @return la respuesta indicando el resultado de la operación (contraseña confirmada exitosamente o error)
     */
    @PostMapping("/confirm-password")
    public ResponseEntity<ApiResponseDTO<ConfirmPasswordResponseDTO>> confirmPassword(
            @Valid @RequestBody ConfirmPasswordRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileMutationService.confirmPassword(request, authHeader));
    }

    /**
     * Elimina el perfil del usuario autenticado.
     *
     * @param authHeader the authorization header
     * @return la respuesta indicando el resultado de la operación (perfil eliminado exitosamente o error)
     */
    @DeleteMapping
    public ResponseEntity<ApiResponseDTO<DeleteProfileResponseDTO>> deleteProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return ResponseEntity.ok(profileMutationService.deleteProfile(authHeader));
    }
}
