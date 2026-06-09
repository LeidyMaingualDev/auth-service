package com.auth.security;

import com.auth.exceptions.BusinessException;
import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.models.enums.SystemRole;
import com.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Componente que obtiene y valida el usuario autenticado a partir de los headers
 * enriquecidos por el Gateway ({@code X-User-Email}, {@code X-Rol}, {@code X-User-Id}).
 *
 * <p>El Gateway ya validó el token JWT antes de reenviar la petición al auth-service.
 * Por eso este guard no vuelve a validar el token — confía en los headers del Gateway
 * y solo verifica que el usuario exista y esté activo en la base de datos.</p>
 *
 * @author Natali Ramirez
 * @version 2.0
 */
@Component
@RequiredArgsConstructor
public class RoleGuard {

    private final UserRepository userRepository;

    /**
     * Obtiene el usuario autenticado y activo a partir del header {@code X-User-Email}
     * inyectado por el Gateway.
     *
     * @param email email del usuario extraído del header X-User-Email
     * @return usuario autenticado y activo
     * @throws BusinessException si el email es nulo, el usuario no existe o está inactivo
     */
    public User getAuthenticatedActiveUser(String email) {
        if (email == null || email.isBlank()) {
            throw BusinessException.unauthorized("Usuario no autenticado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.unauthorized("Usuario no encontrado"));

        if (!user.isActive()) {
            throw new BusinessException("La cuenta se encuentra desactivada", HttpStatus.FORBIDDEN);
        }

        return user;
    }

    /**
     * Verifica que el usuario autenticado tenga el rol requerido.
     *
     * @param email        email del usuario extraído del header X-User-Email
     * @param requiredRole rol requerido para la operación
     * @throws BusinessException si el usuario no tiene el rol requerido
     */
    public void assertHasRole(String email, SystemRole requiredRole) {
        User user = getAuthenticatedActiveUser(email);

        boolean hasRequiredRole = user.getRoles() != null
                && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName -> requiredRole.name().equals(roleName));

        if (!hasRequiredRole) {
            throw new BusinessException(
                    "No tienes permisos para acceder a este recurso", HttpStatus.FORBIDDEN);
        }
    }
}