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

    @Value("${app.max-login-attempts}")
    private int maxLoginAttempts;

    // ─── RF01/RF02 - Registro ─────────────────────────────

    @Transactional
    public ApiResponseDTO<AuthResponseDTO> register(RegisterRequestDTO request) {

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
        user.setActive(true);
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponseDTO authResponse = AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .role("USER")
                .build();

        return ApiResponseDTO.ok("Usuario registrado exitosamente", authResponse);
    }

    // Login con control de intentos
    @Transactional
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request, String ipAddress) {

        // Verificar si el usuario existe y está activo
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ApiResponseDTO.error("Credenciales inválidas");
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

            // Intento exitoso → registrar
            registerLoginAttempt(request.getEmail(), ipAddress, true);

            String token = jwtService.generateToken(user, Boolean.TRUE.equals(request.getRememberMe()));
            String refreshToken = jwtService.generateRefreshToken(user);

            // Obtener rol principal
            String role = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("USER")
                    : "USER";

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(role)
                    .build();

            return ApiResponseDTO.ok("Inicio de sesión exitoso", authResponse);

        } catch (AuthenticationException e) {

            // Intento fallido → registrar
            registerLoginAttempt(request.getEmail(), ipAddress, false);

            // Contar intentos fallidos en los últimos 15 minutos
            long failedAttempts = loginAttemptRepository
                    .countByEmailAndSuccessAndAttemptedAtAfter(
                            request.getEmail(),
                            false,
                            LocalDateTime.now().minusMinutes(15)
                    );

            // Si supera el límite, enviar alerta por email
            if (failedAttempts >= maxLoginAttempts) {
                emailService.sendLoginAlertEmail(user.getEmail(), user.getName(), ipAddress);
                log.warn("Alerta enviada a {} por {} intentos fallidos desde IP: {}",
                        user.getEmail(), failedAttempts, ipAddress);
            }

            return ApiResponseDTO.error("Credenciales inválidas. Intento " +
                    failedAttempts + "/" + maxLoginAttempts);
        }
    }

    // Solicitud de recuperación de contraseña
    @Transactional
    public ApiResponseDTO<Void> forgotPassword(ForgotPasswordRequestDTO request) {

        // Respuesta genérica por seguridad (no revelar si el email existe)
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null && user.isActive()) {
            // Invalidar tokens anteriores
            passwordResetTokenRepository.invalidatePreviousTokens(user.getId());

            // Crear nuevo token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Enviar email
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        }

        // Siempre retorna éxito (seguridad: no revelar si email existe)
        return ApiResponseDTO.ok(
                "Si el correo está registrado, recibirás un enlace en los próximos minutos"
        );
    }

    // Resetear contraseña con token

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

        // Verificar que no sea la misma contraseña
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ApiResponseDTO.error(
                    "La nueva contraseña no puede ser igual a la anterior"
            );
        }

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidar el token usado
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Notificar cambio exitoso
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

        return ApiResponseDTO.ok("Contraseña restablecida exitosamente");
    }

    // Refresh Token

    public ApiResponseDTO<AuthResponseDTO> refreshToken(RefreshTokenRequestDTO request) {

        String refreshToken = request.getRefreshToken();

        // Verificar que no esté en blacklist
        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            return ApiResponseDTO.error("El token no es válido");
        }

        try {
            String email = jwtService.extractUsername(refreshToken);
            var userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ApiResponseDTO.error("El token ha expirado o no es válido");
            }

            String newToken = jwtService.generateToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .email(email)
                    .build();

            return ApiResponseDTO.ok("Token renovado exitosamente", authResponse);

        } catch (Exception e) {
            return ApiResponseDTO.error("Token inválido");
        }
    }

    // Logout seguro
    @Transactional
    public ApiResponseDTO<Void> logout(String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponseDTO.error("Token no proporcionado");
        }

        String token = authHeader.substring(7);

        // Agregar a blacklist
        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .build();

        tokenBlacklistRepository.save(blacklistedToken);

        return ApiResponseDTO.ok("Sesión cerrada exitosamente");
    }

    // Helper: registrar intento de login

    private void registerLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();
        loginAttemptRepository.save(attempt);
    }
}