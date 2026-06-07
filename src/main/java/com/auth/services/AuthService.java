package com.auth.services;

import com.auth.models.dtos.*;
import com.auth.models.entities.*;
import com.auth.models.enums.AuthProvider;
import com.auth.repositories.*;
import com.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio principal de autenticación del sistema Qvenly.
 *
 * <p>Centraliza toda la lógica de negocio relacionada con el ciclo de vida
 * de la autenticación de usuarios:</p>
 * <ul>
 *   <li><b>Registro</b> — crea la cuenta, encripta la contraseña y envía correo de verificación.</li>
 *   <li><b>Confirmación de email</b> — activa la cuenta mediante el token UUID enviado por correo.</li>
 *   <li><b>Login</b> — autentica credenciales, gestiona intentos fallidos y genera tokens JWT.</li>
 *   <li><b>Recuperación de contraseña</b> — genera token de reset y lo envía por correo.</li>
 *   <li><b>Reset de contraseña</b> — valida el token, actualiza la contraseña y notifica al usuario.</li>
 *   <li><b>Renovación de token</b> — valida el refresh token y emite un nuevo par de tokens.</li>
 *   <li><b>Logout</b> — agrega los tokens a la blacklist para invalidarlos inmediatamente.</li>
 * </ul>
 *
 * <p>Este servicio actúa como capa de negocio pura: no maneja HTTP directamente.
 * Toda la comunicación con el cliente pasa por {@code AuthController}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see com.auth.controllers.AuthController
 * @see JwtService
 * @see EmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final EmailService emailService;
    private final UserDetailsService userDetailsService;

    /** Número máximo de intentos fallidos antes de enviar alerta de seguridad. */
    @Value("${app.max-login-attempts}")
    private int maxLoginAttempts;

    // ─────────────────────────────────────────────────────────────────────
    // REGISTRO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo usuario en el sistema con autenticación local.
     *
     * <p>Flujo:</p>
     * <ol>
     *   <li>Verifica que el email y el documento no estén duplicados.</li>
     *   <li>Crea la entidad {@code User} con contraseña BCrypt y token de verificación UUID.</li>
     *   <li>Persiste el usuario con {@code isActive = false} y {@code emailVerified = false}.</li>
     *   <li>Envía el correo de verificación de forma asíncrona.</li>
     * </ol>
     *
     * <p>La cuenta permanece inactiva hasta que el usuario confirme su correo
     * haciendo clic en el enlace enviado.</p>
     *
     * @param request DTO con los datos del nuevo usuario
     * @return respuesta exitosa con mensaje informativo, o error si el email/documento ya existe
     */
    @Transactional
    public ApiResponseDTO<Void> register(RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponseDTO.error("El correo electrónico ya está registrado");
        }

        if (request.getDocumentNumber() != null &&
                userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            return ApiResponseDTO.error("El número de documento ya está registrado");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado en la base de datos"));

        User user = new User();
        user.setName(request.getName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDocumentNumber(request.getDocumentNumber());
        user.setDocumentType(request.getDocumentType());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setActive(false);
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), user.getVerificationToken());

        return ApiResponseDTO.ok("Registro exitoso. Revisa tu correo para confirmar tu cuenta.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Autentica un usuario con correo electrónico y contraseña.
     *
     * <p>Flujo:</p>
     * <ol>
     *   <li>Busca el usuario por email; rechaza si no existe.</li>
     *   <li>Bloquea el acceso si la cuenta fue creada con Google OAuth2.</li>
     *   <li>Verifica que el correo esté confirmado y la cuenta esté activa.</li>
     *   <li>Delega la verificación de credenciales a {@code AuthenticationManager}.</li>
     *   <li>Registra el intento exitoso en {@code login_attempts}.</li>
     *   <li>Genera access token (15 min) y refresh token (1 o 7 días según {@code rememberMe}).</li>
     * </ol>
     *
     * <p>Si las credenciales son incorrectas, registra el intento fallido y envía
     * una alerta de seguridad al usuario si supera el umbral de {@code max-login-attempts}.</p>
     *
     * @param request   DTO con email, contraseña y preferencia de "Recuérdame"
     * @param ipAddress dirección IP del cliente para el registro de intentos
     * @return DTO con los tokens JWT y datos básicos del usuario, o mensaje de error
     */
    @Transactional
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request, String ipAddress) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ApiResponseDTO.error("Credenciales inválidas");
        }

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            return ApiResponseDTO.error(
                    "Esta cuenta fue creada con Google. Usa el botón 'Continuar con Google' para ingresar."
            );
        }

        if (!user.isEmailVerified()) {
            return ApiResponseDTO.error("Debes confirmar tu correo electrónico antes de iniciar sesión.");
        }

        if (!user.isActive()) {
            return ApiResponseDTO.error("Tu cuenta se encuentra desactivada. Contacta con soporte.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            registerLoginAttempt(request.getEmail(), ipAddress, true);

            String role = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream().map(Role::getName).findFirst().orElse("USER")
                    : "USER";

            boolean rememberMe  = Boolean.TRUE.equals(request.getRememberMe());
            String token        = jwtService.generateToken(user, rememberMe, role);
            String refreshToken = jwtService.generateRefreshToken(user, rememberMe);

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(role)
                    .userId(user.getId())
                    .build();

            return ApiResponseDTO.ok("Inicio de sesión exitoso", authResponse);

        } catch (AuthenticationException e) {

            registerLoginAttempt(request.getEmail(), ipAddress, false);

            long failedAttempts = loginAttemptRepository.countByEmailAndSuccessAndAttemptedAtAfter(
                    request.getEmail(), false, LocalDateTime.now().minusMinutes(15)
            );

            if (failedAttempts >= maxLoginAttempts) {
                emailService.sendLoginAlertEmail(user.getEmail(), user.getName(), ipAddress);
                log.warn("Alerta enviada a {} por {} intentos fallidos desde IP: {}",
                        user.getEmail(), failedAttempts, ipAddress);
            }

            return ApiResponseDTO.error("Credenciales inválidas. Intento " + failedAttempts + "/" + maxLoginAttempts);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // RECUPERACIÓN DE CONTRASEÑA
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Inicia el flujo de recuperación de contraseña enviando un enlace al correo del usuario.
     *
     * <p>Por seguridad, la respuesta es siempre la misma independientemente de si el correo
     * existe o no, para evitar la enumeración de usuarios registrados.</p>
     *
     * <p>Si el usuario existe y está activo, invalida tokens previos de recuperación
     * antes de generar uno nuevo con TTL de 30 minutos.</p>
     *
     * @param request DTO con el correo al que enviar el enlace de recuperación
     * @return respuesta exitosa con mensaje genérico (mismo para email existente o no)
     */
    @Transactional
    public ApiResponseDTO<Void> forgotPassword(ForgotPasswordRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null && user.isActive()) {
            passwordResetTokenRepository.invalidatePreviousTokens(user.getId());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        }

        return ApiResponseDTO.ok("Si el correo está registrado, recibirás un enlace en los próximos minutos");
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET DE CONTRASEÑA
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Restablece la contraseña del usuario usando el token de recuperación.
     *
     * <p>Validaciones realizadas en orden:</p>
     * <ol>
     *   <li>Las contraseñas nueva y de confirmación coinciden.</li>
     *   <li>El token existe en la base de datos y no ha sido usado.</li>
     *   <li>El token no ha expirado (TTL de 30 minutos desde su creación).</li>
     *   <li>La nueva contraseña es diferente a la anterior.</li>
     * </ol>
     *
     * <p>Tras el restablecimiento exitoso, marca el token como usado y envía
     * un correo de confirmación al usuario.</p>
     *
     * @param request DTO con el token de recuperación y la nueva contraseña confirmada
     * @return respuesta exitosa o mensaje de error descriptivo según la validación fallida
     */
    @Transactional
    public ApiResponseDTO<Void> resetPassword(ResetPasswordRequestDTO request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponseDTO.error("Las contraseñas no coinciden");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken()).orElse(null);

        if (resetToken == null || resetToken.isUsed()) {
            return ApiResponseDTO.error("El enlace de recuperación no es válido");
        }

        if (resetToken.isExpired()) {
            return ApiResponseDTO.error("El enlace ha expirado. Por favor, solicita uno nuevo");
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ApiResponseDTO.error("La nueva contraseña no puede ser igual a la anterior");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

        return ApiResponseDTO.ok("Contraseña restablecida exitosamente");
    }

    // ─────────────────────────────────────────────────────────────────────
    // RENOVACIÓN DE TOKEN
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Renueva el access token usando un refresh token válido.
     *
     * <p>Llamado desde {@code POST /auth/refresh-from-cookie} cuando el interceptor
     * Angular detecta un {@code 401 Unauthorized} en el frontend.</p>
     *
     * <p>Validaciones:</p>
     * <ol>
     *   <li>El refresh token no está en la blacklist (no fue revocado por logout).</li>
     *   <li>El refresh token no ha expirado y la firma es válida.</li>
     * </ol>
     *
     * <p>Genera un nuevo par de tokens. El nuevo refresh token siempre dura 7 días
     * ya que en este punto no se conoce la preferencia original de "Recuérdame".</p>
     *
     * @param request DTO con el refresh token a validar
     * @return nuevo par de tokens JWT, o error si el refresh token es inválido o expirado
     */
    public ApiResponseDTO<AuthResponseDTO> refreshToken(RefreshTokenRequestDTO request) {

        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            return ApiResponseDTO.error("El token no es válido");
        }

        try {
            String email    = jwtService.extractUsername(refreshToken);
            var userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ApiResponseDTO.error("El token ha expirado o no es válido");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String role = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream().map(Role::getName).findFirst().orElse("USER")
                    : "USER";

            String newToken        = jwtService.generateToken(userDetails, role);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .email(email)
                    .name(user.getName())
                    .role(role)
                    .userId(user.getId())
                    .build();

            return ApiResponseDTO.ok("Token renovado exitosamente", authResponse);

        } catch (Exception e) {
            return ApiResponseDTO.error("Token inválido");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Cierra la sesión del usuario invalidando sus tokens JWT activos.
     *
     * <p>Agrega tanto el access token como el refresh token a la tabla
     * {@code token_blacklist}. Cualquier petición futura con esos tokens
     * será rechazada por {@code JwtAuthFilter} aunque los tokens no hayan expirado.</p>
     *
     * @param authHeader   cabecera {@code Authorization: Bearer <token>} con el access token
     * @param refreshToken cookie {@code refresh_token} a invalidar junto al access token
     * @return respuesta exitosa o error si no se proporcionó cabecera de autorización
     */
    @Transactional
    public ApiResponseDTO<Void> logout(String authHeader, String refreshToken) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponseDTO.error("Token no proporcionado");
        }

        String token = authHeader.substring(7);
        tokenBlacklistRepository.save(TokenBlacklist.builder().token(token).build());

        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenBlacklistRepository.save(TokenBlacklist.builder().token(refreshToken).build());
        }

        return ApiResponseDTO.ok("Sesión cerrada exitosamente");
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONFIRMACIÓN DE EMAIL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Confirma el correo electrónico del usuario y activa su cuenta.
     *
     * <p>Busca al usuario por el token UUID enviado durante el registro.
     * Si es válido y no ha sido usado, establece {@code emailVerified = true},
     * {@code isActive = true} y elimina el token para que no pueda reutilizarse.</p>
     *
     * @param token token UUID de verificación recibido como query param
     * @return respuesta exitosa indicando que ya puede iniciar sesión, o error si el token es inválido
     */
    @Transactional
    public ApiResponseDTO<Void> confirmEmail(String token) {

        User user = userRepository.findByVerificationToken(token).orElse(null);

        if (user == null) {
            return ApiResponseDTO.error("El enlace de confirmación no es válido");
        }

        if (user.isEmailVerified()) {
            return ApiResponseDTO.error("El correo ya fue confirmado anteriormente");
        }

        user.setEmailVerified(true);
        user.setActive(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return ApiResponseDTO.ok("Correo confirmado exitosamente. Ya puedes iniciar sesión.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Registra un intento de inicio de sesión en la tabla {@code login_attempts}.
     *
     * <p>Se llama tanto para intentos exitosos como fallidos, permitiendo
     * detectar patrones de fuerza bruta y generar alertas de seguridad.</p>
     *
     * @param email     correo electrónico del usuario que intentó iniciar sesión
     * @param ipAddress dirección IP desde la que se realizó el intento
     * @param success   {@code true} si el intento fue exitoso; {@code false} si falló
     */
    private void registerLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();
        loginAttemptRepository.save(attempt);
    }
}