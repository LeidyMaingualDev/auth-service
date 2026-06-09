package com.auth.security;

import com.auth.repositories.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que intercepta cada petición HTTP para validar el token JWT.
 *
 * <p>Se ejecuta exactamente una vez por petición gracias a {@link OncePerRequestFilter}
 * y realiza las siguientes comprobaciones en orden:</p>
 * <ol>
 *   <li>Extrae el token JWT de la cabecera {@code Authorization: Bearer <token>}.</li>
 *   <li>Verifica que el token no esté en la blacklist (tokens revocados por logout).</li>
 *   <li>Extrae el email (subject) del payload del token.</li>
 *   <li>Carga los detalles del usuario desde la base de datos.</li>
 *   <li>Valida la firma y la expiración del token.</li>
 *   <li>Si todo es válido, establece la autenticación en el {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>Si cualquier comprobación falla, el filtro deja pasar la petición sin autenticar.
 * Spring Security rechazará la petición en los endpoints protegidos.</p>
 *
 * <p>Las rutas de autenticación ({@code /auth/**}, {@code /oauth2/**})
 * están excluidas del filtro mediante {@link #shouldNotFilter}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see JwtService
 * @see TokenBlacklistRepository
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Servicio para validar y extraer claims de tokens JWT. */
    private final JwtService jwtService;

    /** Servicio para cargar los detalles del usuario desde la base de datos. */
    private final UserDetailsService userDetailsService;

    /** Repositorio para verificar si un token fue revocado por logout. */
    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Lógica principal del filtro JWT. Se invoca una vez por petición HTTP.
     *
     * <p>Si la cabecera {@code Authorization} está ausente o no comienza con {@code "Bearer "},
     * la petición pasa al siguiente filtro sin autenticar. Las rutas públicas continuarán
     * normalmente; las protegidas serán rechazadas por Spring Security más adelante.</p>
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP saliente
     * @param filterChain cadena de filtros a continuar tras la validación
     * @throws ServletException si ocurre un error de servlet durante el filtrado
     * @throws IOException      si ocurre un error de I/O durante el filtrado
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Sin cabecera o sin prefijo Bearer → continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // Token en blacklist → el usuario cerró sesión, continuar sin autenticar
        if (tokenBlacklistRepository.existsByToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extractUsername(jwt);

        // Autenticar solo si hay email y aún no hay autenticación en el contexto
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Excluye del filtrado las rutas de autenticación y OAuth2.
     *
     * <p>Estas rutas no requieren token JWT válido — son el punto de entrada
     * al sistema. Aplicar el filtro sobre ellas causaría errores en el flujo
     * de login y registro.</p>
     *
     * @param request petición HTTP a evaluar
     * @return {@code true} si la ruta debe omitirse; {@code false} si debe filtrarse
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.equals("/auth/forgot-password") ||
                path.equals("/auth/reset-password") ||
                path.equals("/auth/refresh-token") ||
                path.equals("/auth/refresh-from-cookie") ||
                path.equals("/auth/confirm-email") ||
                path.equals("/auth/logout");
    }
}