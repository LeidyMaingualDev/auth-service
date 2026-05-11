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

        // Incluir rol en el token
        String token = jwtService.generateToken(user, "USER");
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

    @Transactional
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request, String ipAddress) {

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

    @Transactional
    public ApiResponseDTO<Void> logout(String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponseDTO.error("Token no proporcionado");
        }

        String token = authHeader.substring(7);

        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .build();

        tokenBlacklistRepository.save(blacklistedToken);

        return ApiResponseDTO.ok("Sesión cerrada exitosamente");
    }

    private void registerLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();
        loginAttemptRepository.save(attempt);
    }
}