package com.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import com.auth.models.entities.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


import java.security.Key;
import java.util.*;
import java.util.function.Function;

/**
 * Servicio responsable de la generación, validación y extracción de claims de tokens JWT.
 *
 * <p>Centraliza toda la lógica relacionada con JWT para que tanto el microservicio
 * de autenticación como el API Gateway puedan reutilizar la misma lógica
 * (el Gateway tiene su propia copia de este servicio).</p>
 *
 * <p>Configuración requerida en {@code application.yaml}:</p>
 * <ul>
 *   <li>{@code jwt.secret} — clave secreta codificada en Base64 (mínimo 256 bits)</li>
 *   <li>{@code jwt.expiration} — duración del access token en milisegundos (ej. 86400000 = 1 día)</li>
 *   <li>{@code jwt.refresh-expiration} — duración del refresh token en ms (ej. 604800000 = 7 días)</li>
 * </ul>
 *
 * <p>Todos los tokens son firmados con el algoritmo {@code HS256} usando la clave
 * HMAC derivada del secreto configurado.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationTime;

    /**
     * Genera un access token JWT sin claims adicionales para el usuario dado.
     *
     * <p>Mantiene compatibilidad con código que no necesita incluir el rol en el token.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @return token JWT firmado con duración estándar ({@code jwt.expiration})
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, expirationTime);
    }

    /**
     * Genera un access token JWT con el rol del usuario incluido como claim.
     *
     * <p>El API Gateway usa el claim {@code role} para tomar decisiones de autorización
     * sin necesidad de consultar el microservicio de autenticación.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @param role        nombre del rol principal del usuario (ej. {@code "USER"}, {@code "ADMIN"})
     * @return token JWT firmado con el claim {@code role} y duración estándar
     */
    public String generateToken(UserDetails userDetails, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userid", ((User) userDetails).getId());
        return buildToken(claims, userDetails, expirationTime);
    }

    /**
     * Genera un access token JWT con rol y duración variable según {@code rememberMe}.
     *
     * <p>Si {@code rememberMe} es {@code true}, el token tendrá la duración del
     * refresh token ({@code jwt.refresh-expiration}) en lugar de la estándar,
     * manteniendo la sesión activa por más tiempo.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @param rememberMe  {@code true} para extender la duración del token
     * @param role        nombre del rol principal del usuario
     * @return token JWT firmado con la duración apropiada según {@code rememberMe}
     */
    public String generateToken(UserDetails userDetails, boolean rememberMe, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", ((User) userDetails).getId());
        long duration = rememberMe ? refreshExpirationTime : expirationTime;
        return buildToken(claims, userDetails, duration);
    }

    /**
     * Genera un refresh token JWT de larga duración.
     *
     * <p>Incluye el claim {@code type: "refresh"} para distinguirlo de los access tokens.
     * Solo debe usarse en el endpoint {@code POST /auth/refresh-token}.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @return refresh token JWT firmado con duración {@code jwt.refresh-expiration}
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, refreshExpirationTime);
    }

    /**
     * Construye y firma un token JWT con los claims, sujeto y tiempos dados.
     *
     * @param extraClaims claims adicionales a incluir en el payload del token
     * @param userDetails usuario cuyo {@code username} (email) será el {@code subject}
     * @param expiration  duración del token en milisegundos desde ahora
     * @return token JWT compacto firmado con HS256
     */
    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida un token JWT verificando que pertenezca al usuario y no haya expirado.
     *
     * @param token       token JWT a validar
     * @param userDetails usuario contra el que se verifica el subject del token
     * @return {@code true} si el token es válido y corresponde al usuario; {@code false} en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Verifica si un token JWT ha expirado.
     *
     * @param token token JWT a verificar
     * @return {@code true} si la fecha de expiración es anterior a la fecha actual
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae el nombre de usuario (email) del subject del token JWT.
     *
     * @param token token JWT del que extraer el email
     * @return correo electrónico del usuario codificado en el token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token token JWT del que extraer la expiración
     * @return fecha y hora de expiración del token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token JWT usando una función resolutora.
     *
     * <p>Permite extraer cualquier claim de forma genérica:</p>
     * <pre>{@code
     * String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
     * }</pre>
     *
     * @param <T>            tipo del claim a extraer
     * @param token          token JWT del que extraer el claim
     * @param claimsResolver función que recibe todos los claims y devuelve el valor deseado
     * @return valor del claim solicitado
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y devuelve todos los claims del token JWT.
     *
     * <p>Lanza una excepción de {@code io.jsonwebtoken} si el token está malformado,
     * ha expirado o la firma no es válida.</p>
     *
     * @param token token JWT a parsear
     * @return objeto {@link Claims} con todos los claims del payload
     * @throws JwtException si el token es inválido o no puede verificarse
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Decodifica la clave secreta Base64 y construye la clave HMAC para firmar y verificar tokens.
     *
     * @return clave criptográfica {@link Key} derivada del secreto configurado
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}