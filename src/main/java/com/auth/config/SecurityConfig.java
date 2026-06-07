package com.auth.config;

import com.auth.security.JwtAuthFilter;
import com.auth.security.OAuth2SuccessHandler;
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
 * Configuración central de seguridad del microservicio de autenticación.
 *
 * <p>Combina dos mecanismos de autenticación:</p>
 * <ul>
 *   <li><b>JWT</b> — para usuarios registrados con correo y contraseña ({@code AuthProvider.LOCAL}).
 *       El filtro {@link JwtAuthFilter} intercepta cada petición y valida el token.</li>
 *   <li><b>Google OAuth2</b> — para usuarios registrados con Google ({@code AuthProvider.GOOGLE}).
 *       El handler {@link OAuth2SuccessHandler} procesa el callback de Google y genera
 *       las cookies HttpOnly con los tokens JWT.</li>
 * </ul>
 *
 * <p>La gestión de sesión usa {@code IF_REQUIRED} para permitir el flujo OAuth2,
 * que requiere sesión HTTP temporal durante el intercambio de código de autorización.
 * Una vez completado el login, los tokens JWT en cookies HttpOnly son la única
 * fuente de autenticación para las peticiones subsiguientes.</p>
 *
 * <p>Las rutas públicas se configuran tanto en este filtro como en el Gateway
 * ({@code application.yaml}) para garantizar coherencia entre ambas capas.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see JwtAuthFilter
 * @see OAuth2SuccessHandler
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filtro que valida el token JWT en cada petición HTTP. */
    private final JwtAuthFilter jwtAuthFilter;

    /** Servicio que carga los detalles del usuario desde la base de datos. */
    private final UserDetailsService userDetailsService;

    /** Handler ejecutado tras un login exitoso con Google OAuth2. */
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    /**
     * Define la cadena de filtros de seguridad HTTP.
     *
     * <p>Configuración aplicada:</p>
     * <ul>
     *   <li>CSRF deshabilitado — la protección la proveen las cookies HttpOnly y SameSite.</li>
     *   <li>Rutas públicas permitidas sin autenticación.</li>
     *   <li>Resto de rutas requieren autenticación válida.</li>
     *   <li>OAuth2 login con handler personalizado y URL de error configurada.</li>
     *   <li>Gestión de sesión {@code IF_REQUIRED} para compatibilidad con OAuth2.</li>
     *   <li>{@link JwtAuthFilter} aplicado antes del filtro de autenticación estándar.</li>
     * </ul>
     *
     * @param http configurador de seguridad HTTP de Spring Security
     * @return cadena de filtros construida y lista para registrarse en el contexto
     * @throws Exception si ocurre un error durante la construcción de la cadena
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/refresh-token",
                                "/auth/refresh-from-cookie",
                                "/auth/confirm-email",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("/auth/login?error=oauth2")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura el proveedor de autenticación basado en base de datos.
     *
     * <p>Usa {@link DaoAuthenticationProvider} con el servicio de usuarios
     * y el codificador BCrypt para verificar las credenciales en el login tradicional.</p>
     *
     * @return proveedor de autenticación configurado
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean de Spring.
     *
     * <p>Requerido por {@code AuthService} para ejecutar la autenticación
     * programáticamente en el endpoint de login.</p>
     *
     * @param config configuración de autenticación de Spring Security
     * @return gestor de autenticación del contexto de Spring Security
     * @throws Exception si no puede obtenerse el gestor de autenticación
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configura el codificador de contraseñas BCrypt.
     *
     * <p>BCrypt aplica un factor de costo adaptativo que ralentiza los ataques
     * de fuerza bruta. Todas las contraseñas del sistema se almacenan con este hash.</p>
     *
     * @return instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura el atributo {@code SameSite=Lax} para todas las cookies del servidor.
     *
     * <p>Este atributo protege contra ataques CSRF al limitar el envío de cookies
     * en peticiones cross-site, permitiéndolas solo en navegación directa de nivel superior.</p>
     *
     * @return proveedor de política SameSite configurado como {@code Lax}
     */
    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax();
    }
}