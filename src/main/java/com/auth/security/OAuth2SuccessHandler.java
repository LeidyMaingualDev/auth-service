package com.auth.security;

import com.auth.models.entities.Role;
import com.auth.models.entities.User;
import com.auth.models.enums.AuthProvider;
import com.auth.models.enums.DocumentType;
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
 * <p>Si el usuario de Google no tiene perfil completo (documento, tipo de documento
 * o teléfono), agrega el parámetro {@code needsProfile=true} al redirect para que
 * el frontend muestre el formulario de completar perfil.</p>
 *
 * @author Leidy Martinez
 * @version 4.0
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

        // Política: no mezclar proveedores LOCAL y GOOGLE
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                log.warn("Intento de login con Google usando email de cuenta LOCAL: {}", email);
                throw new RuntimeException("EMAIL_REGISTERED_LOCALLY");
            }
        });

        // Patrón find-or-create
        boolean isNewUser = userRepository.findByEmail(email).isEmpty();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setLastName(lastName.isBlank() ? "Sin apellido" : lastName);
            newUser.setPassword(null);
            newUser.setProfilePicture(picture);
            newUser.setGoogleId(googleId);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setActive(true);
            newUser.setEmailVerified(Boolean.TRUE.equals(emailVerifiedByGoogle));
            newUser.setRoles(Set.of(userRole));
            // Campos pendientes de completar — valores temporales
            newUser.setDocumentNumber("PENDIENTE");
            newUser.setDocumentType(DocumentType.CC);  // valor temporal, el usuario lo cambiará
            newUser.setPhoneNumber("PENDIENTE");
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

        // Generar tokens
        String accessToken  = jwtService.generateToken(user, false, role);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Escribir tokens como cookies HttpOnly
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(900);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(60 * 60 * 24 * 7);
        response.addCookie(refreshCookie);

        // Detectar si el perfil está incompleto
        boolean needsProfile = !user.isProfileComplete();

        String redirectUrl = String.format(
                "%s/auth/google-callback?name=%s&email=%s&role=%s&userId=%d&needsProfile=%b",
                frontendUrl,
                java.net.URLEncoder.encode(user.getName(), "UTF-8"),
                java.net.URLEncoder.encode(email, "UTF-8"),
                role,
                user.getId(),
                needsProfile
        );

        log.info("Redirigiendo usuario {} — needsProfile: {}", email, needsProfile);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}