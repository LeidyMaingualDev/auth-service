package com.auth.security;

import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;
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
 * Handler ejecutado tras un login exitoso con Google OAuth2.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>Obtiene el email y nombre del usuario desde el perfil de Google.</li>
 *   <li>Si el usuario no existe en BD lo crea automáticamente con rol USER.</li>
 *   <li>Genera un JWT con el rol del usuario.</li>
 *   <li>Redirige al frontend con el token y datos del usuario como query params.</li>
 * </ol>
 *
 * @author Equipo Qvenly
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String givenName = oAuth2User.getAttribute("given_name");
        String fullName  = oAuth2User.getAttribute("name");
        final String name = givenName != null ? givenName : fullName;

        log.info("OAuth2 login exitoso para: {}", email);

        // Buscar o crear el usuario
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "Usuario Google");
            newUser.setLastName("");
            newUser.setPassword("GOOGLE_OAUTH2");
            newUser.setActive(true);
            newUser.setEmailVerified(true);
            newUser.setRoles(Set.of(userRole));
            return userRepository.save(newUser);
        });

        // Obtener rol
        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("USER");

        // Generar JWT
        String token = jwtService.generateToken(user, false, role);

        // Redirigir al frontend con los datos
        String redirectUrl = String.format(
                "%s/auth/google-callback?token=%s&name=%s&email=%s&role=%s&userId=%d",
                frontendUrl,
                token,
                java.net.URLEncoder.encode(user.getName(), "UTF-8"),
                java.net.URLEncoder.encode(email, "UTF-8"),
                role,
                user.getId()
        );

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
