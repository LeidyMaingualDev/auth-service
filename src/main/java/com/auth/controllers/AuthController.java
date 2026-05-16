package com.auth.controllers;

import com.auth.models.dtos.*;
import com.auth.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone los endpoints del ciclo de autenticación de usuarios.
 *
 * <p>Actúa como capa de entrada HTTP; delega toda la lógica de negocio a
 * {@link AuthService} y se limita a mapear el resultado al código de estado HTTP
 * apropiado. Todos los endpoints se publican bajo el prefijo {@code /auth}.</p>
 *
 * <p>Flujo general de autenticación soportado:</p>
 * <ol>
 *   <li>Registro de nuevo usuario → {@code POST /auth/register}</li>
 *   <li>Inicio de sesión → {@code POST /auth/login}</li>
 *   <li>Renovación de token → {@code POST /auth/refresh-token}</li>
 *   <li>Cierre de sesión → {@code POST /auth/logout}</li>
 *   <li>Solicitud de recuperación de contraseña → {@code POST /auth/forgot-password}</li>
 *   <li>Restablecimiento de contraseña → {@code POST /auth/reset-password}</li>
 * </ol>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see AuthService
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Si el registro es exitoso devuelve {@code 201 Created} con el token JWT
     * y el refresh token. Si el correo o documento ya están registrados
     * devuelve {@code 400 Bad Request}.</p>
     *
     * @param request datos de registro validados por Bean Validation
     * @return {@link ApiResponseDTO} con {@link AuthResponseDTO} en caso de éxito,
     *         o con el mensaje de error en caso de fallo
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        ApiResponseDTO<AuthResponseDTO> apiResponse = authService.register(request);
        HttpStatus status = apiResponse.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(apiResponse);
    }

    /**
     * Autentica un usuario con correo electrónico y contraseña.
     *
     * <p>Extrae la dirección IP del cliente para el registro de intentos de inicio
     * de sesión y posibles alertas de seguridad. Devuelve {@code 200 OK} con los
     * tokens JWT en caso de éxito, o {@code 401 Unauthorized} si las credenciales
     * son inválidas.</p>
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
     * enumeración de usuarios.</p>
     *
     * @param request DTO con el correo electrónico del usuario
     * @return {@link ApiResponseDTO} con mensaje informativo
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * Restablece la contraseña del usuario usando el token de recuperación.
     *
     * <p>Valida que el token exista, no haya sido usado y no haya expirado.
     * Devuelve {@code 200 OK} si el restablecimiento es exitoso,
     * o {@code 400 Bad Request} si el token es inválido o las contraseñas no coinciden.</p>
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
     * Renueva el access token usando un refresh token válido.
     *
     * <p>Verifica que el refresh token no esté en la blacklist y no haya expirado.
     * Devuelve un nuevo par de tokens (access + refresh) en caso de éxito,
     * o {@code 401 Unauthorized} si el refresh token es inválido.</p>
     *
     * @param request DTO que contiene el refresh token
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
     * Cierra la sesión del usuario invalidando el token JWT activo.
     *
     * <p>El token puede llegar como cabecera {@code Authorization: Bearer <token>}
     * o como cookie {@code access_token}. El token es agregado a la blacklist
     * para que no pueda reutilizarse aunque no haya expirado.</p>
     *
     * @param authHeader  cabecera HTTP {@code Authorization} (opcional)
     * @param cookieToken cookie {@code access_token} como alternativa al header (opcional)
     * @return {@link ApiResponseDTO} confirmando el cierre de sesión
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {

        String token = authHeader;
        if (token == null && cookieToken != null) {
            token = "Bearer " + cookieToken;
        }

        return ResponseEntity.ok(authService.logout(token));
    }

    /**
     * Extrae la dirección IP real del cliente considerando proxies y balanceadores de carga.
     *
     * <p>Primero intenta leer la cabecera {@code X-Forwarded-For} (presente cuando
     * la petición pasa por un proxy o API Gateway). Si no existe, usa
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