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
 * <p>Es usado en dos contextos distintos:</p>
 * <ul>
 *   <li><b>Login tradicional</b> — {@code DaoAuthenticationProvider} lo llama para
 *       cargar el usuario y verificar la contraseña durante la autenticación.</li>
 *   <li><b>Validación JWT</b> — {@code JwtAuthFilter} lo llama para recargar los
 *       detalles del usuario desde la base de datos al validar el token en cada petición.</li>
 * </ul>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see UserRepository
 * @see com.auth.security.JwtAuthFilter
 * @see com.auth.config.SecurityConfig
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /** Repositorio para consultar usuarios en la base de datos. */
    private final UserRepository userRepository;

    /**
     * Carga un usuario de la base de datos usando su correo electrónico como identificador.
     *
     * <p>En Qvenly el "username" de Spring Security es el correo electrónico,
     * no un nombre de usuario tradicional. La entidad {@code User} implementa
     * {@link UserDetails} directamente, por lo que se devuelve sin transformación.</p>
     *
     * @param email correo electrónico del usuario a cargar
     * @return entidad {@code User} con toda la información de autenticación y autorización
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