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
 * <p>Soporta dos modos de autenticación:</p>
 * <ol>
 *   <li><b>Header X-User-Email</b> — inyectado por el Gateway tras validar el JWT.
 *       Es el modo principal cuando las peticiones pasan por el Gateway.</li>
 *   <li><b>Header Authorization: Bearer</b> — para peticiones directas al auth-service
 *       sin pasar por el Gateway (Postman, pruebas, etc.).</li>
 * </ol>
 *
 * @author Leidy Martinez
 * @version 4.0
 * @see JwtService
 * @see TokenBlacklistRepository
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Modo 1: Header X-User-Email inyectado por el Gateway ──────────
        String emailFromGateway = request.getHeader("X-User-Email");
        if (emailFromGateway != null && !emailFromGateway.isBlank()) {
            authenticateByEmail(emailFromGateway, request);
            filterChain.doFilter(request, response);
            return;
        }

        // ── Modo 2: Header Authorization: Bearer (acceso directo) ─────────
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        if (tokenBlacklistRepository.existsByToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extractUsername(jwt);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                setAuthentication(userDetails, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Autentica al usuario en el SecurityContext usando su email.
     * Usado cuando el Gateway ya validó el JWT e inyectó X-User-Email.
     */
    private void authenticateByEmail(String email, HttpServletRequest request) {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                setAuthentication(userDetails, request);
            }
        } catch (Exception e) {
            logger.warn("No se pudo autenticar por X-User-Email: " + e.getMessage());
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

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