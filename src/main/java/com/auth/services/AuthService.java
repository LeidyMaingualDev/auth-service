package com.auth.services;

import com.auth.models.dtos.*;
import com.auth.models.entities.*;
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
 * Servicio central de autenticación y gestión del ciclo de vida de las sesiones de usuario.
 *
 * <p>Implementa los seis casos de uso principales de autenticación:</p>
 * <ol>
 *   <li><strong>Registro</strong>: valida unicidad de email y documento, asigna rol {@code USER},
 *       genera y devuelve los tokens JWT.</li>
 *   <li><strong>Login</strong>: autentica con {@code AuthenticationManager}, registra el intento,
 *       envía alerta si se supera el umbral de fallos, devuelve tokens con el rol incluido.</li>
 *   <li><strong>Forgot password</strong>: genera un token UUID de un solo uso, lo persiste con TTL
 *       de 30 minutos, invalida tokens anteriores del usuario y envía el correo de recuperación.</li>
 *   <li><strong>Reset password</strong>: valida el token, verifica que la nueva contraseña
 *       sea distinta a la anterior, actualiza el hash en base de datos y envía confirmación.</li>
 *   <li><strong>Refresh token</strong>: verifica que el refresh token no esté en blacklist ni expirado,
 *       genera un nuevo par de tokens y los devuelve.</li>
 *   <li><strong>Logout</strong>: agrega el access token a la blacklist para invalidarlo de inmediato.</li>
 * </ol>
 *
 * <p>Todas las operaciones que modifican la base de datos están anotadas con {@code @Transactional}
 * para garantizar la consistencia ante fallos parciales.</p>
 *
 * @author Equipo Qvenly
 * @version 1.0
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

    /** Número máximo de intentos fallidos antes de enviar alerta de seguridad. Configurable en {@code app.max-login-attempts}. */
    @Value("${app.max-login-attempts}")
    private int maxLoginAttempts;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Pasos del proceso:</p>
     * <ol>
     *   <li>Verifica que el correo y el número de documento no estén ya registrados.</li>
     *   <li>Carga el rol {@code "USER"} desde la base de datos (debe existir previamente).</li>
     *   <li>Construye la entidad {@code User} con la contraseña codificada con BCrypt.</li>
     *   <li>Persiste el usuario y genera un par de tokens JWT (access + refresh).</li>
     * </ol>
     *
     * @param request DTO con los datos del nuevo usuario (validado previamente con {@code @Valid})
     * @return {@link ApiResponseDTO} con los tokens JWT en caso de éxito,
     *         o con el mensaje de error si el correo o documento ya existen
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
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(), user.getName(), user.getVerificationToken()
        );

        return ApiResponseDTO.ok(
                "Registro exitoso. Revisa tu correo para confirmar tu cuenta."
        );
    }

    /**
     * Autentica un usuario verificando sus credenciales y registrando el intento.
     *
     * <p>Pasos del proceso:</p>
     * <ol>
     *   <li>Verifica que el usuario exista y que su cuenta esté activa.</li>
     *   <li>Delega la verificación de credenciales a {@code AuthenticationManager}.</li>
     *   <li>En caso de éxito: registra el intento como exitoso y genera los tokens.</li>
     *   <li>En caso de fallo: registra el intento fallido; si supera {@code maxLoginAttempts}
     *       en los últimos 15 minutos, envía una alerta de seguridad al correo del usuario.</li>
     * </ol>
     *
     * <p>El rol principal del usuario se incluye como claim {@code "role"} en el token
     * para que el API Gateway pueda tomar decisiones de autorización sin consultar
     * este microservicio en cada petición.</p>
     *
     * @param request   DTO con email, contraseña y flag {@code rememberMe}
     * @param ipAddress IP del cliente, usada para el registro del intento y la alerta de seguridad
     * @return {@link ApiResponseDTO} con los tokens JWT en caso de éxito,
     *         o con el mensaje de error y el conteo de intentos en caso de fallo
     */
    @Transactional
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request, String ipAddress) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ApiResponseDTO.error("Credenciales inválidas");
        }

        if (!user.isEmailVerified()) {
            return ApiResponseDTO.error(
                    "Debes confirmar tu correo electrónico antes de iniciar sesión."
            );
        }

        if (!user.isActive()) {
            return ApiResponseDTO.error("Tu cuenta se encuentra desactivada. Contacta con soporte.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()
                    )
            );

            registerLoginAttempt(request.getEmail(), ipAddress, true);

            // Obtener rol principal
            String role = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("USER")
                    : "USER";

            // Incluir rol en el token
            String token = jwtService.generateToken(user, Boolean.TRUE.equals(request.getRememberMe()), role);
            String refreshToken = jwtService.generateRefreshToken(user);

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

            long failedAttempts = loginAttemptRepository
                    .countByEmailAndSuccessAndAttemptedAtAfter(
                            request.getEmail(),
                            false,
                            LocalDateTime.now().minusMinutes(15)
                    );

            if (failedAttempts >= maxLoginAttempts) {
                emailService.sendLoginAlertEmail(user.getEmail(), user.getName(), ipAddress);
                log.warn("Alerta enviada a {} por {} intentos fallidos desde IP: {}",
                        user.getEmail(), failedAttempts, ipAddress);
            }

            return ApiResponseDTO.error("Credenciales inválidas. Intento " +
                    failedAttempts + "/" + maxLoginAttempts);
        }
    }

    /**
     * Inicia el flujo de recuperación de contraseña enviando un enlace al correo del usuario.
     *
     * <p>Por diseño de seguridad, siempre devuelve el mismo mensaje de éxito independientemente
     * de si el correo existe o no, para evitar la enumeración de usuarios registrados.</p>
     *
     * <p>Si el usuario existe y está activo:</p>
     * <ol>
     *   <li>Se invalidan los tokens de recuperación anteriores del usuario.</li>
     *   <li>Se genera un nuevo token UUID con TTL de 30 minutos.</li>
     *   <li>Se persiste el token y se envía el correo de forma asíncrona.</li>
     * </ol>
     *
     * @param request DTO con el correo electrónico del usuario
     * @return {@link ApiResponseDTO} siempre exitoso con mensaje genérico
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

        return ApiResponseDTO.ok(
                "Si el correo está registrado, recibirás un enlace en los próximos minutos"
        );
    }

    /**
     * Restablece la contraseña del usuario usando el token de recuperación de un solo uso.
     *
     * <p>Validaciones realizadas:</p>
     * <ul>
     *   <li>Las contraseñas nueva y de confirmación deben coincidir.</li>
     *   <li>El token debe existir en base de datos y no haber sido usado.</li>
     *   <li>El token no debe haber expirado (TTL de 30 minutos).</li>
     *   <li>La nueva contraseña debe ser diferente a la contraseña actual.</li>
     * </ul>
     *
     * <p>Tras el restablecimiento exitoso, el token se marca como usado y se envía
     * un correo de confirmación al usuario.</p>
     *
     * @param request DTO con el token UUID, la nueva contraseña y su confirmación
     * @return {@link ApiResponseDTO} exitoso si el restablecimiento fue correcto,
     *         o con el mensaje de error específico si alguna validación falla
     */
    @Transactional
    public ApiResponseDTO<Void> resetPassword(ResetPasswordRequestDTO request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponseDTO.error("Las contraseñas no coinciden");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElse(null);

        if (resetToken == null || resetToken.isUsed()) {
            return ApiResponseDTO.error("El enlace de recuperación no es válido");
        }

        if (resetToken.isExpired()) {
            return ApiResponseDTO.error(
                    "El enlace ha expirado. Por favor, solicita uno nuevo"
            );
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ApiResponseDTO.error(
                    "La nueva contraseña no puede ser igual a la anterior"
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

        return ApiResponseDTO.ok("Contraseña restablecida exitosamente");
    }

    /**
     * Renueva el access token usando un refresh token válido.
     *
     * <p>Verifica que el refresh token no esté en la blacklist (revocado) y no haya expirado.
     * Si es válido, genera un nuevo par de tokens (access + refresh) con el rol actualizado
     * del usuario desde la base de datos.</p>
     *
     * @param request DTO con el refresh token a renovar
     * @return {@link ApiResponseDTO} con el nuevo par de tokens en caso de éxito,
     *         o con el mensaje de error si el token es inválido o ha expirado
     */
    public ApiResponseDTO<AuthResponseDTO> refreshToken(RefreshTokenRequestDTO request) {

        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            return ApiResponseDTO.error("El token no es válido");
        }

        try {
            String email = jwtService.extractUsername(refreshToken);
            var userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ApiResponseDTO.error("El token ha expirado o no es válido");
            }

            // Obtener rol del usuario para incluirlo en el nuevo token
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String role = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("USER")
                    : "USER";

            String newToken = jwtService.generateToken(userDetails, role);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .email(email)
                    .role(role)
                    .build();

            return ApiResponseDTO.ok("Token renovado exitosamente", authResponse);

        } catch (Exception e) {
            return ApiResponseDTO.error("Token inválido");
        }
    }

    /**
     * Cierra la sesión del usuario invalidando el access token y el refresh token.
     *
     * <p>Ambos tokens se agregan a la tabla {@code token_blacklist}. Esto garantiza
     * que aunque el refresh token no haya expirado, no pueda usarse para obtener
     * un nuevo access token tras el logout.</p>
     *
     * @param authHeader   valor completo de la cabecera {@code Authorization} (con prefijo {@code "Bearer "})
     * @param refreshToken refresh token a invalidar (opcional, puede ser {@code null})
     * @return {@link ApiResponseDTO} confirmando el cierre de sesión,
     *         o con mensaje de error si no se proporcionó el access token
     */
    @Transactional
    public ApiResponseDTO<Void> logout(String authHeader, String refreshToken) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponseDTO.error("Token no proporcionado");
        }

        String token = authHeader.substring(7);

        tokenBlacklistRepository.save(
                TokenBlacklist.builder().token(token).build()
        );

        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenBlacklistRepository.save(
                    TokenBlacklist.builder().token(refreshToken).build()
            );
        }

        return ApiResponseDTO.ok("Sesión cerrada exitosamente");
    }

    /**
     * Registra un intento de inicio de sesión en la base de datos para auditoría.
     *
     * <p>Se llama tanto en intentos exitosos como fallidos para mantener un historial
     * completo de la actividad de autenticación.</p>
     *
     * @param email     correo electrónico con el que se intentó iniciar sesión
     * @param ipAddress dirección IP del cliente
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

    /**
     * Confirma el correo electrónico del usuario usando el token de verificación.
     *
     * <p>Activa la cuenta ({@code isActive = true}) y elimina el token de verificación
     * una vez usado. Si el token no existe o ya fue usado devuelve error.</p>
     *
     * @param token token UUID enviado al correo del usuario durante el registro
     * @return {@link ApiResponseDTO} exitoso si la confirmación fue correcta
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
}