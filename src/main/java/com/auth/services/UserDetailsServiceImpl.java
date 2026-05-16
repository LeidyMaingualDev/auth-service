package com.auth.services;

import com.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link UserDetailsService} que carga usuarios desde la base de datos.
 *
 * <p>Spring Security requiere esta implementación para autenticar usuarios mediante
 * {@code DaoAuthenticationProvider}. Al usar el correo electrónico como "username",
 * este servicio sirve de puente entre el sistema de autenticación de Spring y la
 * entidad {@code User} persistida en base de datos.</p>
 *
 * <p>También es usado por {@code JwtAuthFilter} para recargar los detalles del usuario
 * desde la base de datos al validar un token JWT en cada petición.</p>
 *
 * @author Equipo Qvenly
 * @version 1.0
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga un usuario de la base de datos usando su correo electrónico como identificador.
     *
     * <p>En este microservicio el "username" de Spring Security es el correo electrónico,
     * no un nombre de usuario tradicional.</p>
     *
     * @param email correo electrónico del usuario a cargar
     * @return objeto {@link UserDetails} con la información del usuario (la entidad {@code User})
     * @throws UsernameNotFoundException si no existe ningún usuario con ese correo electrónico
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));
    }
}
