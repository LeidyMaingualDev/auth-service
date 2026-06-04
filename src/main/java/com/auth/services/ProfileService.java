package com.auth.services;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio para gestionar el perfil del usuario, incluye operaciones para consultar y actualizar la información del perfil. Utiliza el RoleGuard para verificar la autenticación y autorización del usuario antes de permitir el acceso a las operaciones relacionadas con el perfil. Proporciona métodos para mapear la entidad User a un DTO de respuesta de perfil de usuario, y para resolver el rol principal del usuario en función de sus roles asignados.
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
     * @param authHeader el encabezado de autenticación
     * @return el perfil del usuario autenticado
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<UserProfileResponseDTO> getAuthenticatedProfile(String authHeader) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);

        return ApiResponseDTO.ok(
                "Perfil consultado exitosamente",
                mapToUserProfileResponse(user)
        );
    }

    /**
     * Mapea la entidad User a un DTO de respuesta de perfil de usuario.
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
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Resuelve el rol principal del usuario en función de sus roles asignados.
     * @param roles la lista de roles del usuario
     * @return el rol principal del usuario
     */
    private String resolveMainRole(List<String> roles) {
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("ORGANIZER")) {
            return "ORGANIZER";
        }
        if (roles.contains("USER")) {
            return "USER";
        }
        return roles.isEmpty() ? null : roles.get(0);
    }
}
