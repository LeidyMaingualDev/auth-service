package com.auth.services;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.SessionSecurityResponseDTO;
import com.auth.models.dtos.UserProfileResponseDTO;
import com.auth.models.entities.LoginAttempt;
import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.repositories.LoginAttemptRepository;
import com.auth.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio para gestionar el perfil del usuario autenticado.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final RoleGuard roleGuard;
    private final LoginAttemptRepository loginAttemptRepository;

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @param authHeader encabezado de autenticacion
     * @return perfil del usuario autenticado
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
     * Obtiene informacion de sesion y seguridad basada en datos reales registrados.
     *
     * @param authHeader encabezado de autenticacion
     * @param userAgent encabezado User-Agent de la solicitud actual
     * @param currentIp IP detectada en la solicitud actual
     * @return datos de sesion y seguridad
     */
    @Transactional(readOnly = true)
    public ApiResponseDTO<SessionSecurityResponseDTO> getSessionSecurity(
            String authHeader,
            String userAgent,
            String currentIp) {
        User user = roleGuard.getAuthenticatedActiveUser(authHeader);
        Optional<LoginAttempt> lastLogin = findLastSuccessfulLogin(user);

        String ipAddress = lastLogin
                .map(LoginAttempt::getIpAddress)
                .filter(ip -> ip != null && !ip.isBlank())
                .orElse(currentIp);

        LocalDateTime startedAt = lastLogin
                .map(LoginAttempt::getAttemptedAt)
                .orElse(user.getCreatedAt());

        SessionSecurityResponseDTO response = SessionSecurityResponseDTO.builder()
                .device(resolveDevice(userAgent))
                .ipAddress(ipAddress)
                .startedAt(startedAt)
                .build();

        return ApiResponseDTO.ok("Sesion consultada exitosamente", response);
    }

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
        LocalDateTime lastAccess = findLastSuccessfulLogin(user)
                .map(LoginAttempt::getAttemptedAt)
                .orElse(null);

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
                .lastAccess(lastAccess)
                .build();
    }

    private Optional<LoginAttempt> findLastSuccessfulLogin(User user) {
        return loginAttemptRepository.findFirstByEmailAndSuccessOrderByAttemptedAtDesc(user.getEmail(), true);
    }

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

    private String resolveDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo no identificado";
        }

        String normalized = userAgent.toLowerCase();
        String browser = "Navegador Web";
        if (normalized.contains("edg/")) {
            browser = "Microsoft Edge";
        } else if (normalized.contains("chrome/")) {
            browser = "Google Chrome";
        } else if (normalized.contains("firefox/")) {
            browser = "Mozilla Firefox";
        } else if (normalized.contains("safari/") && !normalized.contains("chrome/")) {
            browser = "Safari";
        }

        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return browser + " movil";
        }

        return browser;
    }
}