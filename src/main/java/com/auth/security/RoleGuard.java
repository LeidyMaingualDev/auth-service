package com.auth.security.roles;

import com.auth.exceptions.BusinessException;
import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.models.enums.SystemRole;
import com.auth.repositories.TokenBlacklistRepository;
import com.auth.repositories.UserRepository;
import com.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Clase que implementa la lógica de control de roles para la seguridad, verifica si el usuario autenticado tiene los roles necesarios para acceder a ciertos recursos protegidos. Utiliza el servicio de JWT para validar el token y extraer la información del usuario, y el repositorio de usuarios para obtener los detalles del usuario autenticado.
 * 
 * @author Natali Ramirez
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class RoleGuard {

    /** El prefijo del token Bearer */
    private static final String BEARER_PREFIX = "Bearer ";

    /** El servicio de JWT */
    private final JwtService jwtService;
    /** El repositorio de usuarios */
    private final UserRepository userRepository;
    /** El repositorio de lista negra de tokens */
    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Obtiene el usuario autenticado y activo.
     * @param authHeader el encabezado de autenticación
     * @return el usuario autenticado y activo
     */
    public User getAuthenticatedActiveUser(String authHeader) {
        String token = extractBearerToken(authHeader);

        if (tokenBlacklistRepository.existsByToken(token)) {
            throw BusinessException.unauthorized("Token invalido o sesion cerrada");
        }

        String email = extractValidEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.unauthorized("Token invalido o usuario no encontrado"));

        if (!user.isActive()) {
            throw new BusinessException("La cuenta se encuentra desactivada", HttpStatus.FORBIDDEN);
        }

        return user;
    }

    /**
     * Verifica si el usuario autenticado tiene el rol requerido.
     * @param authHeader el encabezado de autenticación
     * @param requiredRole el rol requerido
     */
    public void assertHasRole(String authHeader, SystemRole requiredRole) {
        User user = getAuthenticatedActiveUser(authHeader);

        boolean hasRequiredRole = user.getRoles() != null
                && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName -> requiredRole.name().equals(roleName));

        if (!hasRequiredRole) {
            throw new BusinessException("No tienes permisos para acceder a este recurso", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Extrae el token Bearer del encabezado de autenticación.
     * @param authHeader el encabezado de autenticación
     * @return el token Bearer
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw BusinessException.unauthorized("Token no proporcionado");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw BusinessException.unauthorized("Token no proporcionado");
        }

        return token;
    }

    /**
     * Extrae el correo electrónico válido del token.
     * @param token el token JWT
     * @return el correo electrónico válido
     */
    private String extractValidEmail(String token) {
        try {
            String email = jwtService.extractUsername(token);
            if (email == null || email.isBlank() || jwtService.isTokenExpired(token)) {
                throw BusinessException.unauthorized("Token invalido o expirado");
            }
            return email;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.unauthorized("Token invalido o expirado");
        }
    }
}
