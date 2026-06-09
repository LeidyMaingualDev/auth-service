package com.auth.services;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.models.enums.AuthProvider;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio para consultar el perfil del usuario autenticado.
 *
 * <p>Utiliza {@link RoleGuard} para extraer y validar el JWT antes de
 * devolver los datos del perfil. Soporta tanto usuarios locales como
 * usuarios registrados con Google OAuth2.</p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final RoleGuard roleGuard;

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @param authHeader encabezado Authorization con token Bearer
     * @return perfil del usuario autenticado
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<UserProfileResponseDTO> getAuthenticatedProfile(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        return ApiResponseDTO.ok("Perfil consultado exitosamente", mapToUserProfileResponse(user));
    }

    /**
     * Mapea la entidad User al DTO de respuesta del perfil.
     *
     * @param user entidad del usuario autenticado
     * @return DTO con los datos del perfil
     */
    public UserProfileResponseDTO mapToUserProfileResponse(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        String fullName = String.format("%s %s",
                user.getName() != null ? user.getName() : "",
                user.getLastName() != null ? user.getLastName() : ""
        ).trim();

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
                .profilePicture(user.getProfilePicture())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Resuelve el rol principal del usuario.
     * En Qvenly solo existen ADMIN y USER.
     *
     * @param roles lista de roles del usuario
     * @return rol principal
     */
    private String resolveMainRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("USER")) return "USER";
        return roles.isEmpty() ? null : roles.get(0);
    }
}