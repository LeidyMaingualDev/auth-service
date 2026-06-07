package com.auth.controllers;

import com.auth.models.dtos.*;
import com.auth.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone los endpoints del ciclo de autenticación de usuarios.
 *
 * <p>Actúa como capa de entrada HTTP; delega toda la lógica de negocio a
 * {@link AuthService} y se limita a mapear el resultado al código de estado HTTP
 * apropiado. Todos los endpoints se publican bajo el prefijo {@code /auth}.</p>
 *
 * <p>Endpoints disponibles:</p>
 * <ol>
 *   <li>Registro de nuevo usuario          → {@code POST /auth/register}</li>
 *   <li>Confirmación de correo             → {@code GET  /auth/confirm-email}</li>
 *   <li>Inicio de sesión                   → {@code POST /auth/login}</li>
 *   <li>Recuperación de contraseña         → {@code POST /auth/forgot-password}</li>
 *   <li>Restablecimiento de contraseña     → {@code POST /auth/reset-password}</li>
 *   <li>Renovación de token (body)         → {@code POST /auth/refresh-token}</li>
 *   <li>Renovación de token (cookie)       → {@code POST /auth/refresh-from-cookie}</li>
 *   <li>Cierre de sesión                   → {@code POST /auth/logout}</li>
 * </ol>
 *
 * <p>El login con Google OAuth2 no pasa por este controlador — es manejado
 * directamente por Spring Security y {@code OAuth2SuccessHandler}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see AuthService
 * @see com.auth.security.OAuth2SuccessHandler
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** URL base del frontend, usada para construir redirecciones si fuera necesario. */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Registra un nuevo usuario en el sistema con autenticación local.
     *
     * <p>Si el registro es exitoso devuelve {@code 201 Created} con un mensaje
     * indicando que debe verificar su correo. La cuenta permanece inactiva hasta
     * la confirmación del email. Devuelve {@code 400 Bad Request} si el correo
     * o el documento ya están registrados.</p>
     *
     * @param request datos de registro validados por Bean Validation
     * @return {@link ApiResponseDTO} con mensaje de éxito o de error
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        ApiResponseDTO<Void> apiResponse = authService.register(request);
        HttpStatus status = apiResponse.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(apiResponse);
    }

    /**
     * Confirma el correo electrónico del usuario usando el token de verificación.
     *
     * <p>El usuario llega aquí desde el enlace enviado a su correo tras el registro.
     * Si el token es válido activa la cuenta ({@code isActive = true}, {@code emailVerified = true})
     * e invalida el token para que no pueda reutilizarse.</p>
     *
     * <p>Devuelve {@code 200 OK} si la confirmación es exitosa,
     * o {@code 400 Bad Request} si el token no existe o ya fue usado.</p>
     *
     * @param token token UUID de verificación recibido como query param ({@code ?token=...})
     * @return {@link ApiResponseDTO} con resultado de la confirmación
     */
    @GetMapping("/confirm-email")
    public ResponseEntity<ApiResponseDTO<Void>> confirmEmail(
            @RequestParam String token) {

        ApiResponseDTO<Void> response = authService.confirmEmail(token);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Autentica un usuario con correo electrónico y contraseña.
     *
     * <p>Extrae la dirección IP del cliente para el registro de intentos de inicio
     * de sesión y posibles alertas de seguridad.</p>
     *
     * <p>Devuelve {@code 200 OK} con los tokens JWT en caso de éxito.
     * Los tokens son interceptados por el {@code AuthResponseCookieFilter} del Gateway,
     * que los convierte en cookies {@code HttpOnly} y los elimina del body antes
     * de enviarlo al cliente.</p>
     *
     * <p>Devuelve {@code 401 Unauthorized} si las credenciales son inválidas,
     * el correo no está verificado, la cuenta está desactivada o el usuario
     * fue registrado con Google OAuth2.</p>
     *
     * @param request     credenciales de inicio de sesión (email, password, rememberMe)
     * @param httpRequest solicitud HTTP original, usada para extraer la IP del cliente
     * @return {@link ApiResponseDTO} con {@link AuthResponseDTO} en caso de éxito
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        ApiResponseDTO<AuthResponseDTO> apiResponse = authService.login(request, ipAddress);
        HttpStatus status = apiResponse.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(apiResponse);
    }

    /**
     * Inicia el flujo de recuperación de contraseña enviando un correo con el enlace de reset.
     *
     * <p>Por razones de seguridad, siempre devuelve {@code 200 OK} con el mismo
     * mensaje independientemente de si el correo existe o no, para evitar la
     * enumeración de usuarios registrados.</p>
     *
     * @param request DTO con el correo electrónico del usuario
     * @return {@link ApiResponseDTO} con mensaje informativo genérico
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * Restablece la contraseña del usuario usando el token de recuperación.
     *
     * <p>Valida que el token exista, no haya sido usado y no haya expirado (TTL 30 min).
     * También verifica que la nueva contraseña sea diferente a la anterior.</p>
     *
     * <p>Devuelve {@code 200 OK} si el restablecimiento es exitoso,
     * o {@code 400 Bad Request} si el token es inválido, expirado o las
     * contraseñas no coinciden.</p>
     *
     * @param request DTO con el token de recuperación y la nueva contraseña confirmada
     * @return {@link ApiResponseDTO} con el resultado de la operación
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        ApiResponseDTO<Void> response = authService.resetPassword(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Renueva el access token usando un refresh token enviado en el body de la petición.
     *
     * <p>Endpoint alternativo a {@link #refreshFromCookie} para clientes que no
     * soportan cookies (ej. aplicaciones móviles o clientes API directos).</p>
     *
     * <p>Devuelve un nuevo par de tokens (access + refresh) en caso de éxito,
     * o {@code 401 Unauthorized} si el refresh token es inválido o ha expirado.</p>
     *
     * @param request DTO con el refresh token a validar
     * @return {@link ApiResponseDTO} con {@link AuthResponseDTO} con los nuevos tokens
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        ApiResponseDTO<AuthResponseDTO> response = authService.refreshToken(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Renueva el access token leyendo el refresh token directamente de la cookie HttpOnly.
     *
     * <p>Es el endpoint que usa el interceptor Angular ({@code TokenInterceptor}) cuando
     * detecta un {@code 401 Unauthorized} en una petición. Al leer el token de la cookie
     * HttpOnly, el frontend nunca necesita acceder al valor del token directamente,
     * manteniéndolo protegido de JavaScript.</p>
     *
     * <p>La respuesta pasa por el {@code AuthResponseCookieFilter} del Gateway,
     * que actualiza las cookies {@code access_token} y {@code refresh_token}
     * con los nuevos valores.</p>
     *
     * <p>Devuelve {@code 401 Unauthorized} si la cookie no existe o el token es inválido.</p>
     *
     * @param refreshTokenCookie cookie {@code refresh_token} enviada automáticamente por el navegador
     * @return {@link ApiResponseDTO} con los nuevos tokens en caso de éxito
     */
    @PostMapping("/refresh-from-cookie")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> refreshFromCookie(
            @CookieValue(value = "refresh_token", required = false) String refreshTokenCookie) {

        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error("No hay sesión activa"));
        }

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshTokenCookie);
        ApiResponseDTO<AuthResponseDTO> response = authService.refreshToken(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Cierra la sesión del usuario invalidando sus tokens JWT activos.
     *
     * <p>Acepta el token de dos fuentes (en orden de prioridad):</p>
     * <ol>
     *   <li>Cabecera {@code Authorization: Bearer <token>}</li>
     *   <li>Cookie {@code access_token} (como alternativa cuando no se puede leer el header)</li>
     * </ol>
     *
     * <p>Ambos tokens (access y refresh) son agregados a la blacklist para que no
     * puedan reutilizarse aunque no hayan expirado aún.</p>
     *
     * @param authHeader         cabecera HTTP {@code Authorization} con el access token (opcional)
     * @param cookieToken        cookie {@code access_token} como alternativa al header (opcional)
     * @param refreshTokenCookie cookie {@code refresh_token} a invalidar junto al access token (opcional)
     * @return {@link ApiResponseDTO} confirmando el cierre de sesión
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken,
            @CookieValue(value = "refresh_token", required = false) String refreshTokenCookie) {

        String token = authHeader;
        if (token == null && cookieToken != null) {
            token = "Bearer " + cookieToken;
        }

        return ResponseEntity.ok(authService.logout(token, refreshTokenCookie));
    }

    /**
     * Extrae la dirección IP real del cliente considerando proxies y balanceadores de carga.
     *
     * <p>Primero intenta leer la cabecera {@code X-Forwarded-For}, presente cuando
     * la petición pasa por el API Gateway. Si no existe, usa
     * {@link HttpServletRequest#getRemoteAddr()} como fallback.</p>
     *
     * @param request solicitud HTTP de la que se extrae la IP
     * @return dirección IP del cliente como cadena de texto
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}