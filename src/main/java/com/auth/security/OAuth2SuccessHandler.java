package com.auth.security;

import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.models.enums.AuthProvider;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Handler ejecutado por Spring Security tras un inicio de sesión exitoso con Google OAuth2.
 *
 * <p>Implementa el patrón <b>find-or-create</b>: busca al usuario por email en la base
 * de datos y, si no existe, crea una cuenta nueva con los datos del perfil de Google.
 * Si el usuario ya existe, actualiza su foto de perfil si cambió en Google.</p>
 *
 * <p>Aplica la política de <b>no mezclar proveedores</b>: si el email ya está registrado
 * con {@code AuthProvider.LOCAL}, el acceso mediante Google es bloqueado y el usuario
 * es redirigido al login con un mensaje de error claro.</p>
 *
 * <p>Datos extraídos del perfil OAuth2 de Google:</p>
 * <ul>
 *   <li>{@code sub} → {@code googleId} — identificador único e inmutable del usuario en Google.</li>
 *   <li>{@code given_name} → {@code name} — nombre de pila.</li>
 *   <li>{@code family_name} → {@code lastName} — apellido(s).</li>
 *   <li>{@code picture} → {@code profilePicture} — URL de la foto de perfil.</li>
 *   <li>{@code email_verified} → {@code emailVerified} — indica si Google verificó el correo.</li>
 * </ul>
 *
 * <p>El token JWT <b>no viaja en la URL</b> (lo que sería un riesgo de seguridad).
 * En su lugar, se escribe directamente como cookie {@code HttpOnly} en la respuesta.
 * Solo datos no sensibles ({@code name}, {@code email}, {@code role}, {@code userId})
 * se envían como query params al frontend para personalizar la UI.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see JwtService
 * @see UserRepository
 * @see AuthProvider
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /** Servicio para generar los tokens JWT tras el login exitoso. */
    private final JwtService jwtService;

    /** Repositorio para buscar y persistir usuarios. */
    private final UserRepository userRepository;

    /** Repositorio para obtener el rol USER al crear cuentas nuevas. */
    private final RoleRepository roleRepository;

    /** URL base del frontend, configurada en {@code application.yaml} como {@code app.frontend-url}. */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Punto de entrada del handler. Invocado por Spring Security tras autenticación OAuth2 exitosa.
     *
     * <p>Delega la lógica a {@link #handleGoogleLogin} y captura el caso de conflicto
     * de proveedores para redirigir al login con un mensaje de error en lugar de lanzar
     * una excepción no controlada.</p>
     *
     * @param request        petición HTTP del callback de Google
     * @param response       respuesta HTTP donde se escriben las cookies y el redirect
     * @param authentication objeto de autenticación con el perfil OAuth2 del usuario
     * @throws IOException si ocurre un error al escribir el redirect
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        try {
            handleGoogleLogin(request, response, oAuth2User);
        } catch (RuntimeException e) {
            if ("EMAIL_REGISTERED_LOCALLY".equals(e.getMessage())) {
                String errorUrl = frontendUrl + "/auth/login?error=email_registered_locally";
                getRedirectStrategy().sendRedirect(request, response, errorUrl);
            } else {
                throw e;
            }
        }
    }

    /**
     * Lógica principal del flujo de login con Google OAuth2.
     *
     * <p>Ejecuta en orden:</p>
     * <ol>
     *   <li>Extrae los atributos del perfil OAuth2 de Google.</li>
     *   <li>Verifica que el email no esté registrado con {@code AuthProvider.LOCAL}.</li>
     *   <li>Busca el usuario por email o crea uno nuevo con los datos de Google.</li>
     *   <li>Actualiza la foto de perfil si cambió en Google.</li>
     *   <li>Genera tokens JWT y los escribe como cookies {@code HttpOnly}.</li>
     *   <li>Redirige al frontend con datos no sensibles como query params.</li>
     * </ol>
     *
     * @param request    petición HTTP del callback de Google
     * @param response   respuesta HTTP donde se escriben las cookies y el redirect
     * @param oAuth2User perfil del usuario autenticado con Google
     * @throws IOException si ocurre un error al escribir el redirect
     */
    private void handleGoogleLogin(HttpServletRequest request,
                                   HttpServletResponse response,
                                   OAuth2User oAuth2User) throws IOException {

        String email                  = oAuth2User.getAttribute("email");
        String googleId               = oAuth2User.getAttribute("sub");
        String givenName              = oAuth2User.getAttribute("given_name");
        String familyName             = oAuth2User.getAttribute("family_name");
        String fullName               = oAuth2User.getAttribute("name");
        String picture                = oAuth2User.getAttribute("picture");
        Boolean emailVerifiedByGoogle = oAuth2User.getAttribute("email_verified");

        final String name     = givenName  != null ? givenName  : (fullName != null ? fullName : "Usuario");
        final String lastName = familyName != null ? familyName : "";

        log.info("OAuth2 login exitoso para: {} (googleId: {})", email, googleId);

        // Política Opción A: nunca mezclar proveedores LOCAL y GOOGLE
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                log.warn("Intento de login con Google usando email de cuenta LOCAL: {}", email);
                throw new RuntimeException("EMAIL_REGISTERED_LOCALLY");
            }
        });

        // Patrón find-or-create: buscar usuario existente o crear uno nuevo
        User user = userRepository.findByEmail(email).orElseGet(() -> {

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setLastName(lastName);
            newUser.setPassword(null);
            newUser.setProfilePicture(picture);
            newUser.setGoogleId(googleId);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setActive(true);
            newUser.setEmailVerified(Boolean.TRUE.equals(emailVerifiedByGoogle));
            newUser.setRoles(Set.of(userRole));
            return userRepository.save(newUser);
        });

        // Actualizar foto de perfil si cambió en Google
        if (picture != null && !picture.equals(user.getProfilePicture())) {
            user.setProfilePicture(picture);
            userRepository.save(user);
        }

        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("USER");

        // Generar tokens — refresh token siempre 7 días para usuarios de Google
        String accessToken  = jwtService.generateToken(user, false, role);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Escribir tokens como cookies HttpOnly directamente en la respuesta
        // (el redirect no pasa por el AuthResponseCookieFilter del Gateway)
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(900);             // 15 minutos
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(60 * 60 * 24 * 7); // 7 días
        response.addCookie(refreshCookie);

        // Redirigir al frontend con datos no sensibles — el token no va en la URL
        String redirectUrl = String.format(
                "%s/auth/google-callback?name=%s&email=%s&role=%s&userId=%d",
                frontendUrl,
                java.net.URLEncoder.encode(user.getName(), "UTF-8"),
                java.net.URLEncoder.encode(email, "UTF-8"),
                role,
                user.getId()
        );

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}