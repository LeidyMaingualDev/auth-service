package com.auth.services;

import com.auth.exceptions.BusinessException;
import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.ChangePasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordRequestDTO;
import com.auth.models.dtos.ConfirmPasswordResponseDTO;
import com.auth.models.dtos.DeleteProfileResponseDTO;
import com.auth.models.dtos.UpdateProfileRequestDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.models.entities.Role;
import com.auth.models.entities.TokenBlacklist;
import com.auth.models.entities.User;
import com.auth.models.enums.DocumentType;
import com.auth.repositories.TokenBlacklistRepository;
import com.auth.repositories.UserRepository;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio para realizar mutaciones en el perfil del usuario, como
 * actualización de información y cambio de contraseña, y para manejar la
 * eliminación lógica del perfil. Utiliza el RoleGuard para verificar la
 * autenticación y autorización del usuario antes de permitir las mutaciones en
 * el perfil. Proporciona métodos para actualizar el perfil del usuario, cambiar
 * la contraseña, confirmar la contraseña actual y eliminar el perfil, manejando
 * las validaciones necesarias y publicando notificaciones relevantes a través
 * del ProfileNotificationPublisherService cuando se realizan cambios en el
 * perfil del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ProfileMutationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final ProfileNotificationPublisherService notificationPublisherService;
    private final RoleGuard roleGuard;

    /**
     * Actualiza la información del perfil del usuario.
     * 
     * @param request    los datos de actualización del perfil
     * @param authHeader el encabezado de autenticación
     * @return la respuesta con el perfil actualizado
     */
    @Transactional
    public ApiResponseDTO<UserProfileResponseDTO> updateProfile(
            UpdateProfileRequestDTO request,
            String authHeader) {

        User user = getActiveAuthenticatedUser(authHeader);

        String newEmail = normalizeRequired(request.getEmail());
        String newDocumentNumber = normalizeOptional(request.getDocumentNumber());
        boolean emailChanged = !newEmail.equalsIgnoreCase(user.getEmail());
        boolean documentChanged = !Objects.equals(newDocumentNumber, user.getDocumentNumber());

        if (emailChanged) {
            userRepository.findByEmail(newEmail)
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw BusinessException.conflict("El correo electronico ya esta registrado");
                    });
        }

        if (documentChanged
                && newDocumentNumber != null
                && userRepository.existsByDocumentNumber(newDocumentNumber)) {
            throw BusinessException.conflict("El numero de documento ya esta registrado");
        }

        user.setName(normalizeRequired(request.getName()));
        user.setLastName(normalizeRequired(request.getLastName()));
        user.setEmail(newEmail);
        user.setPhoneNumber(normalizeOptional(request.getPhoneNumber()));
        user.setDocumentType(parseDocumentType(request.getDocumentType()));
        user.setDocumentNumber(newDocumentNumber);

        User updatedUser = userRepository.save(user);

        if (emailChanged) {
            blacklistBearerToken(authHeader);
        }

        return ApiResponseDTO.ok(
                "Perfil actualizado exitosamente",
                mapToUserProfileResponse(updatedUser));
    }

    /**
     * Cambia la contraseña del usuario.
     * 
     * @param request    los datos de cambio de contraseña
     * @param authHeader el encabezado de autenticación
     * @return la respuesta con el resultado del cambio de contraseña
     */
    @Transactional
    public ApiResponseDTO<Void> changePassword(ChangePasswordRequestDTO request, String authHeader) {
        User user = getActiveAuthenticatedUser(authHeader);

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

        notificationPublisherService.publishPasswordChanged(user);

        return ApiResponseDTO.ok("Contrasena actualizada exitosamente");
    }

    /**
     * Confirma la contraseña del usuario.
     * 
     * @param request    los datos de confirmación de contraseña
     * @param authHeader el encabezado de autenticación
     * @return la respuesta con el resultado de la confirmación
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<ConfirmPasswordResponseDTO> confirmPassword(
            ConfirmPasswordRequestDTO request,
            String authHeader) {
        User user = getActiveAuthenticatedUser(authHeader);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("La contrasena es incorrecta", HttpStatus.FORBIDDEN);
        }

        ConfirmPasswordResponseDTO response = ConfirmPasswordResponseDTO.builder()
                .valid(true)
                .validatedAt(LocalDateTime.now())
                .build();

        return ApiResponseDTO.ok("Contrasena validada correctamente", response);
    }

    /**
     * Elimina el perfil del usuario de forma lógica.
     * 
     * @param authHeader el encabezado de autenticación
     * @return la respuesta con el resultado de la eliminación
     */
    @Transactional
    public ApiResponseDTO<DeleteProfileResponseDTO> deleteProfile(String authHeader) {
        User user = getActiveAuthenticatedUser(authHeader);

        user.setActive(false);
        userRepository.save(user);
        blacklistBearerToken(authHeader);
        notificationPublisherService.publishProfileDeleted(user);

        LocalDateTime timestamp = LocalDateTime.now();
        DeleteProfileResponseDTO response = DeleteProfileResponseDTO.builder()
                .deleted(true)
                .deletionType("LOGICAL")
                .operationId(UUID.randomUUID().toString())
                .timestamp(timestamp)
                .build();

        return ApiResponseDTO.ok("Perfil eliminado exitosamente", response);
    }

    /**
     * Obtiene el usuario autenticado y activo.
     * 
     * @param authHeader el encabezado de autenticación
     * @return el usuario autenticado y activo
     */
    private User getActiveAuthenticatedUser(String authHeader) {
        return roleGuard.getAuthenticatedActiveUser(authHeader);
    }

    /**
     * Mapea la entidad User a un DTO de respuesta de perfil de usuario.
     * 
     * @param user la entidad User
     * @return el DTO de respuesta de perfil de usuario
     */
    private UserProfileResponseDTO mapToUserProfileResponse(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();

        String fullName = String.format("%s %s",
                user.getName() != null ? user.getName() : "",
                user.getLastName() != null ? user.getLastName() : "").trim();

        boolean active = user.isActive();

        return UserProfileResponseDTO.builder()
                .id(user.getId())
                .fullName(fullName)
                .name(user.getName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(resolveMainRole(roles))
                .roles(roles)
                .active(active)
                .status(active ? "ACTIVO" : "INACTIVO")
                .documentType(user.getDocumentType() != null ? user.getDocumentType().name() : null)
                .documentNumber(user.getDocumentNumber())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Resuelve el rol principal del usuario en función de sus roles asignados.
     * 
     * @param roles la lista de roles del usuario
     * @return el rol principal del usuario
     */
    private String resolveMainRole(List<String> roles) {
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("USER")) {
            return "USER";
        }
        return roles.isEmpty() ? null : roles.get(0);
    }

    /**
     * Analiza el tipo de documento y lo convierte en una instancia de DocumentType.
     * 
     * @param documentType el tipo de documento como cadena
     * @return la instancia de DocumentType correspondiente
     */
    private DocumentType parseDocumentType(String documentType) {
        String normalizedDocumentType = normalizeOptional(documentType);
        if (normalizedDocumentType == null) {
            return null;
        }

        try {
            return DocumentType.valueOf(normalizedDocumentType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Tipo de documento no valido");
        }
    }

    /**
     * Normaliza un valor requerido, eliminando espacios en blanco y devolviendo
     * null si es vacío.
     * 
     * @param value el valor a normalizar
     * @return el valor normalizado o null si es vacío
     */
    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Normaliza un valor opcional, eliminando espacios en blanco y devolviendo null
     * si es vacío.
     * 
     * @param value el valor a normalizar
     * @return el valor normalizado o null si es vacío
     */
    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Añade un token de acceso a la lista negra.
     *
     * @param authHeader el encabezado de autenticación
     */
    private void blacklistBearerToken(String authHeader) {
        String token = extractBearerToken(authHeader);

        if (token == null || token.isBlank()) {
            return;
        }

        if (tokenBlacklistRepository.existsByToken(token)) {
            return;
        }

        TokenBlacklist tokenBlacklist = TokenBlacklist.builder()
                .token(token)
                .build();

        tokenBlacklistRepository.save(
                Objects.requireNonNull(tokenBlacklist, "El token de lista negra no puede ser null"));
    }

    /**
     * Extrae el token de acceso del encabezado de autenticación.
     * 
     * @param authHeader el encabezado de autenticación
     * @return el token de acceso o null si no es válido
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
