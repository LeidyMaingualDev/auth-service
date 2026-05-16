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
 * <p>Se ejecuta exactamente una vez por petición ({@link OncePerRequestFilter}) y
 * realiza las siguientes comprobaciones en orden:</p>
 * <ol>
 *   <li>Extrae el token JWT de la cabecera {@code Authorization: Bearer <token>}.</li>
 *   <li>Verifica que el token no esté en la blacklist (tokens revocados por logout).</li>
 *   <li>Extrae el email (subject) del token.</li>
 *   <li>Carga los detalles del usuario desde la base de datos.</li>
 *   <li>Valida la firma y la expiración del token.</li>
 *   <li>Si todo es válido, establece la autenticación en el {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>Si cualquier comprobación falla, el filtro deja pasar la petición sin autenticar
 * y Spring Security la rechazará en los endpoints protegidos.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see JwtService
 * @see TokenBlacklistRepository
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistRepository tokenBlacklistRepository; // RF06

    /**
     * Lógica principal del filtro JWT. Se invoca una vez por petición HTTP.
     *
     * <p>Si la cabecera {@code Authorization} está ausente o no empieza con {@code "Bearer "},
     * la petición se pasa al siguiente filtro sin autenticar (las rutas públicas continuarán
     * normalmente; las protegidas serán rechazadas por Spring Security más adelante).</p>
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP saliente
     * @param filterChain cadena de filtros a continuar si la validación es correcta
     * @throws ServletException si ocurre un error de servlet
     * @throws IOException      si ocurre un error de I/O
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

        // Token en blacklist → continuar sin autenticar (el usuario cerró sesión)
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
}