package com.auth.services;

import com.auth.exceptions.BusinessException;
import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.ChangePasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordResponseDTO;
import com.auth.models.dtos.DeleteProfileRequestDTO;
import com.auth.models.dtos.DeleteProfileResponseDTO;
import com.auth.models.dtos.UpdateProfileRequestDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.models.entities.TokenBlacklist;
import com.auth.models.entities.User;
import com.auth.models.enums.AuthProvider;
import com.auth.models.enums.DocumentType;
import com.auth.repositories.TokenBlacklistRepository;
import com.auth.repositories.UserRepository;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio para realizar mutaciones en el perfil del usuario:
 * actualización de datos, cambio de contraseña, confirmación y eliminación.
 *
 * <p>Utiliza {@link RoleGuard} para extraer y validar el JWT antes de
 * ejecutar cualquier operación. Los usuarios de Google OAuth2 no pueden
 * cambiar contraseña ya que no tienen una propia del sistema.</p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileMutationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final ProfileNotificationPublisherService notificationPublisherService;
    private final NotificationInboxService notificationInboxService;
    private final UserUnlinkingService userUnlinkingService;
    private final RoleGuard roleGuard;
    private final ProfileService profileService;

    /**
     * Actualiza los datos personales del perfil del usuario autenticado.
     *
     * <p>Si el email cambia, el token actual se invalida para forzar
     * un nuevo inicio de sesión con el nuevo correo.</p>
     *
     * @param request    datos de actualización del perfil
     * @param authHeader encabezado Authorization con token Bearer
     * @return perfil actualizado
     */
    @Transactional
    public ApiResponseDTO<UserProfileResponseDTO> updateProfile(
            UpdateProfileRequestDTO request,
            String authHeader) {

        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        String newName           = normalizeRequired(request.getName());
        String newLastName       = normalizeRequired(request.getLastName());
        String newEmail          = normalizeRequired(request.getEmail());
        String newPhoneNumber    = normalizeOptional(request.getPhoneNumber());
        String newDocumentNumber = normalizeOptional(request.getDocumentNumber());
        DocumentType newDocumentType = parseDocumentType(request.getDocumentType());

        boolean emailChanged    = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());
        boolean documentChanged = !Objects.equals(newDocumentNumber, user.getDocumentNumber());

        List<String> changedFields = detectChangedFields(
                user, newName, newLastName, newEmail,
                newPhoneNumber, newDocumentType, newDocumentNumber);

        if (emailChanged) {
            userRepository.findByEmail(newEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw BusinessException.conflict("El correo electronico ya esta registrado");
                    });
        }

        if (documentChanged && newDocumentNumber != null
                && userRepository.existsByDocumentNumber(newDocumentNumber)) {
            throw BusinessException.conflict("El numero de documento ya esta registrado");
        }

        user.setName(newName);
        user.setLastName(newLastName);
        user.setEmail(newEmail);
        user.setPhoneNumber(newPhoneNumber);
        user.setDocumentType(newDocumentType);
        user.setDocumentNumber(newDocumentNumber);

        User updatedUser = userRepository.save(user);

        if (emailChanged) {
            blacklistBearerToken(authHeader);
        }

        if (!changedFields.isEmpty()) {
            try {
                notificationInboxService.createProfileUpdatedNotification(updatedUser, changedFields);
            } catch (Exception e) {
                log.error("Error al registrar notificacion de actualizacion para usuario {}: {}",
                        updatedUser.getId(), e.getMessage());
            }
        }

        return ApiResponseDTO.ok("Perfil actualizado exitosamente",
                profileService.mapToUserProfileResponse(updatedUser));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>Solo aplica para usuarios con {@code AuthProvider.LOCAL}.
     * Los usuarios de Google OAuth2 no pueden usar este endpoint.</p>
     *
     * @param request    datos del cambio de contraseña
     * @param authHeader encabezado Authorization con token Bearer
     * @return confirmación del cambio
     */
    @Transactional
    public ApiResponseDTO<Void> changePassword(ChangePasswordRequestDTO request, String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        if (AuthProvider.GOOGLE.equals(user.getAuthProvider())) {
            throw BusinessException.badRequest(
                    "Los usuarios registrados con Google no pueden cambiar su contrasena desde aqui");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("La contrasena actual es incorrecta", HttpStatus.FORBIDDEN);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw BusinessException.badRequest("Las contrasenas no coinciden");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.badRequest("La nueva contrasena no puede ser igual a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        blacklistBearerToken(authHeader);
        notificationPublisherService.publishPasswordChanged(user);

        return ApiResponseDTO.ok("Contrasena actualizada exitosamente");
    }

    /**
     * Confirma la contraseña del usuario como paso previo a una operación sensible.
     *
     * @param request    datos de confirmación
     * @param authHeader encabezado Authorization con token Bearer
     * @return confirmación con timestamp
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<ConfirmPasswordResponseDTO> confirmPassword(
            ConfirmPasswordRequestDTO request,
            String authHeader) {

        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("La contrasena es incorrecta", HttpStatus.FORBIDDEN);
        }

        return ApiResponseDTO.ok("Contrasena validada correctamente",
                ConfirmPasswordResponseDTO.builder()
                        .valid(true)
                        .validatedAt(LocalDateTime.now())
                        .build());
    }

    /**
     * Elimina definitivamente el perfil del usuario tras confirmar su contraseña.
     *
     * <p>Envía notificación por correo antes de la eliminación, invalida el token
     * y elimina el registro de la base de datos.</p>
     *
     * @param request    solicitud con la contraseña actual del usuario
     * @param authHeader encabezado Authorization con token Bearer
     * @return resultado de la eliminación
     */
    @Transactional
    public ApiResponseDTO<DeleteProfileResponseDTO> deleteProfile(
            DeleteProfileRequestDTO request,
            String authHeader) {

        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("La contrasena es incorrecta", HttpStatus.FORBIDDEN);
        }

        notificationPublisherService.publishProfileDeleted(user);
        blacklistBearerToken(authHeader);
        userUnlinkingService.unlinkLocalUserRelations(user);
        userRepository.delete(user);
        userRepository.flush();

        return ApiResponseDTO.ok("Perfil eliminado exitosamente",
                DeleteProfileResponseDTO.builder()
                        .deleted(true)
                        .deletionType("PHYSICAL")
                        .operationId(UUID.randomUUID().toString())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────

    private List<String> detectChangedFields(User user, String newName, String newLastName,
                                             String newEmail, String newPhoneNumber,
                                             DocumentType newDocumentType, String newDocumentNumber) {

        List<String> changed = new ArrayList<>();
        if (!Objects.equals(newName, user.getName()))                   changed.add("nombre");
        if (!Objects.equals(newLastName, user.getLastName()))           changed.add("apellido");
        if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) changed.add("correo");
        if (!Objects.equals(newPhoneNumber, user.getPhoneNumber()))     changed.add("telefono");
        if (!Objects.equals(newDocumentType, user.getDocumentType()))   changed.add("tipo de documento");
        if (!Objects.equals(newDocumentNumber, user.getDocumentNumber())) changed.add("numero de documento");
        return changed;
    }

    private DocumentType parseDocumentType(String documentType) {
        String normalized = normalizeOptional(documentType);
        if (normalized == null) return null;
        try {
            return DocumentType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Tipo de documento no valido");
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void blacklistBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return;
        String token = authHeader.substring(7);
        if (token.isBlank() || tokenBlacklistRepository.existsByToken(token)) return;
        tokenBlacklistRepository.save(TokenBlacklist.builder().token(token).build());
    }
}