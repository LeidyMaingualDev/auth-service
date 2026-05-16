package com.auth.config;

import com.auth.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de seguridad de Spring Security para el microservicio de autenticación.
 *
 * <p>Define la cadena de filtros HTTP, la política de sesiones sin estado (stateless),
 * el proveedor de autenticación basado en base de datos y el registro del filtro JWT.
 * Las rutas públicas (registro, login, recuperación de contraseña, actuator) se
 * declaran explícitamente; cualquier otra ruta requiere autenticación válida.</p>
 *
 * <p>La política de cookies {@code SameSite=None} se configura para permitir el envío
 * de cookies en contextos cross-origin, necesario cuando el frontend Angular
 * (puerto 4200) se comunica con el API Gateway en un origen distinto.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see JwtAuthFilter
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Construye y registra la cadena de filtros de seguridad HTTP.
     *
     * <p>Configuración aplicada:</p>
     * <ul>
     *   <li>CSRF deshabilitado (la autenticación es stateless vía JWT).</li>
     *   <li>Rutas públicas: {@code /auth/register}, {@code /auth/login},
     *       {@code /auth/forgot-password}, {@code /auth/reset-password},
     *       {@code /auth/refresh-token}, {@code /actuator/health}, {@code /actuator/info}.</li>
     *   <li>Todas las demás rutas requieren autenticación.</li>
     *   <li>Sesiones HTTP deshabilitadas ({@code STATELESS}).</li>
     *   <li>{@link JwtAuthFilter} se ejecuta antes de {@code UsernamePasswordAuthenticationFilter}.</li>
     * </ul>
     *
     * @param http objeto {@link HttpSecurity} proporcionado por Spring Security
     * @return la cadena de filtros configurada
     * @throws Exception si ocurre un error al construir la cadena
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/refresh-token",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Rutas protegidas
                        .anyRequest().authenticated()
                )
                // Sin sesiones
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Autenticación
                .authenticationProvider(authenticationProvider())
                // Filtro JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Crea el proveedor de autenticación basado en base de datos (DAO).
     *
     * <p>Utiliza {@link UserDetailsService} para cargar los detalles del usuario
     * y {@link PasswordEncoder} para verificar la contraseña codificada con BCrypt.</p>
     *
     * @return proveedor de autenticación configurado con el servicio de usuarios y el encoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} de Spring Security como bean de contexto.
     *
     * <p>Es requerido por {@code AuthService} para autenticar las credenciales
     * durante el proceso de inicio de sesión.</p>
     *
     * @param config configuración de autenticación de Spring
     * @return el {@link AuthenticationManager} activo
     * @throws Exception si no se puede obtener el manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define el codificador de contraseñas usando el algoritmo BCrypt.
     *
     * <p>BCrypt incorpora salt automáticamente y aplica un factor de coste
     * que dificulta los ataques de fuerza bruta.</p>
     *
     * @return instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura la política {@code SameSite=None} para las cookies de sesión.
     *
     * <p>Necesario para entornos cross-origin donde el frontend Angular
     * y el API Gateway se encuentran en dominios u orígenes distintos.</p>
     *
     * @return proveedor de la política SameSite para las cookies
     */
    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofNone();
    }
}