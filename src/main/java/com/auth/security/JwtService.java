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
 * <p>Centraliza toda la lógica relacionada con JWT para que el microservicio
 * de autenticación pueda reutilizarla de forma coherente. El API Gateway
 * tiene su propia copia de este servicio para validar tokens sin depender
 * del microservicio de autenticación.</p>
 *
 * <p>Configuración requerida en {@code application.yaml}:</p>
 * <ul>
 *   <li>{@code jwt.secret} — clave secreta codificada en Base64 (mínimo 256 bits).</li>
 *   <li>{@code jwt.expiration} — duración del access token en ms. Valor: {@code 900000} (15 min).</li>
 *   <li>{@code jwt.refresh-expiration} — duración máxima del refresh token en ms.
 *       Valor: {@code 604800000} (7 días).</li>
 * </ul>
 *
 * <p>Lógica de duración según "Recuérdame":</p>
 * <ul>
 *   <li>Access token — siempre 15 minutos, independiente del valor de {@code rememberMe}.</li>
 *   <li>Refresh token — 1 día si {@code rememberMe = false}; 7 días si {@code rememberMe = true}.</li>
 * </ul>
 *
 * <p>Todos los tokens son firmados con el algoritmo {@code HS256} usando la clave
 * HMAC derivada del secreto configurado.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see JwtAuthFilter
 */
@Service
public class JwtService {

    /** Clave secreta codificada en Base64 para firmar y verificar tokens JWT. */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Duración del access token en milisegundos. Valor configurado: 900000 (15 minutos). */
    @Value("${jwt.expiration}")
    private long expirationTime;

    /** Duración máxima del refresh token en milisegundos. Valor configurado: 604800000 (7 días). */
    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationTime;

    /**
     * Duración del refresh token cuando el usuario NO marcó "Recuérdame".
     * La sesión expira al día siguiente aunque el navegador siga abierto.
     */
    private static final long REFRESH_NO_REMEMBER_MS = 86400000L;

    // ─────────────────────────────────────────────────────────────────────
    // GENERACIÓN DE TOKENS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Genera un access token JWT sin claims adicionales.
     *
     * <p>Mantiene compatibilidad con código que no necesita incluir el rol en el token.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @return token JWT firmado con duración de 15 minutos
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, expirationTime);
    }

    /**
     * Genera un access token JWT con el rol del usuario incluido como claim.
     *
     * <p>El API Gateway usa el claim {@code role} para tomar decisiones de autorización
     * sin necesidad de consultar el microservicio de autenticación en cada petición.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @param role        nombre del rol principal del usuario (ej. {@code "USER"}, {@code "ADMIN"})
     * @return token JWT firmado con los claims {@code role} y {@code userId}, duración 15 minutos
     */
    public String generateToken(UserDetails userDetails, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", ((User) userDetails).getId());
        return buildToken(claims, userDetails, expirationTime);
    }

    /**
     * Genera un access token JWT con rol y parámetro {@code rememberMe}.
     *
     * <p>El access token <b>siempre dura 15 minutos</b> independientemente del valor
     * de {@code rememberMe}. El parámetro solo afecta la duración del refresh token
     * (ver {@link #generateRefreshToken(UserDetails, boolean)}).</p>
     *
     * @param userDetails datos del usuario autenticado
     * @param rememberMe  no afecta el access token; incluido por compatibilidad con la firma
     * @param role        nombre del rol principal del usuario
     * @return token JWT firmado con los claims {@code role} y {@code userId}, duración 15 minutos
     */
    public String generateToken(UserDetails userDetails, boolean rememberMe, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", ((User) userDetails).getId());
        return buildToken(claims, userDetails, expirationTime);
    }

    /**
     * Genera un refresh token JWT con duración fija de 7 días.
     *
     * <p>Usado en contextos donde no se conoce el valor de {@code rememberMe},
     * como el flujo de Google OAuth2 o la renovación automática del interceptor.</p>
     *
     * @param userDetails datos del usuario autenticado
     * @return refresh token JWT con el claim {@code type: "refresh"} y duración de 7 días
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, refreshExpirationTime);
    }

    /**
     * Genera un refresh token JWT con duración variable según la preferencia del usuario.
     *
     * <p>Este es el método que debe usarse en el login tradicional para respetar
     * la preferencia de "Recuérdame" del usuario:</p>
     * <ul>
     *   <li>{@code rememberMe = true} → 7 días. El usuario puede cerrar el navegador
     *       y retomar la sesión sin re-autenticarse.</li>
     *   <li>{@code rememberMe = false} → 1 día. La sesión expira al día siguiente.</li>
     * </ul>
     *
     * @param userDetails datos del usuario autenticado
     * @param rememberMe  {@code true} para sesión de 7 días; {@code false} para sesión de 1 día
     * @return refresh token JWT con el claim {@code type: "refresh"} y la duración correspondiente
     */
    public String generateRefreshToken(UserDetails userDetails, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        long duration = rememberMe ? refreshExpirationTime : REFRESH_NO_REMEMBER_MS;
        return buildToken(claims, userDetails, duration);
    }

    // ─────────────────────────────────────────────────────────────────────
    // VALIDACIÓN
    // ─────────────────────────────────────────────────────────────────────

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
     * Verifica si un token JWT ha expirado comparando su fecha de expiración con la actual.
     *
     * @param token token JWT a verificar
     * @return {@code true} si la fecha de expiración es anterior a la fecha actual
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXTRACCIÓN DE CLAIMS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Extrae el nombre de usuario (email) del subject del token JWT.
     *
     * @param token token JWT del que extraer el email
     * @return correo electrónico del usuario codificado en el subject del token
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
     * String role   = jwtService.extractClaim(token, c -> c.get("role", String.class));
     * Long   userId = jwtService.extractClaim(token, c -> c.get("userId", Long.class));
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
     * Parsea y devuelve todos los claims del payload del token JWT.
     *
     * <p>Lanza una excepción de {@code io.jsonwebtoken} si el token está malformado,
     * ha expirado o la firma no es válida con la clave configurada.</p>
     *
     * @param token token JWT a parsear
     * @return objeto {@link Claims} con todos los claims del payload
     * @throws JwtException si el token es inválido, expirado o no puede verificarse
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERNO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construye y firma un token JWT con los claims, sujeto y tiempo de expiración dados.
     *
     * @param extraClaims claims adicionales a incluir en el payload del token
     * @param userDetails usuario cuyo email será el {@code subject} del token
     * @param expiration  duración del token en milisegundos desde el momento de emisión
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
     * Decodifica la clave secreta Base64 y construye la clave HMAC para firmar y verificar tokens.
     *
     * @return clave criptográfica {@link Key} derivada del secreto configurado en {@code jwt.secret}
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}