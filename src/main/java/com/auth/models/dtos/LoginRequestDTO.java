package com.auth.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de solicitud para el inicio de sesión de un usuario existente.
 *
 * <p>Contiene las credenciales mínimas requeridas para autenticarse, más la opción
 * de mantener la sesión activa mediante {@code rememberMe}.</p>
 *
 * <p>Efecto de {@code rememberMe} sobre los tokens generados:</p>
 * <ul>
 *   <li><b>Access token</b> — siempre 15 minutos, independiente del valor de {@code rememberMe}.</li>
 *   <li><b>Refresh token</b> — 1 día si {@code rememberMe = false}; 7 días si {@code rememberMe = true}.</li>
 * </ul>
 *
 * <p>Solo aplica para usuarios registrados con {@code AuthProvider.LOCAL}.
 * Los usuarios de Google OAuth2 no usan este endpoint.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see com.auth.models.enums.AuthProvider
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    /** Correo electrónico del usuario. Debe tener formato válido de email. */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    /** Contraseña del usuario. No se valida el formato aquí — ya fue validado en el registro. */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Indica si el usuario desea mantener la sesión activa por más tiempo.
     * Por defecto {@code false}.
     * Cuando es {@code true}, el refresh token dura 7 días en lugar de 1 día,
     * permitiendo que el usuario vuelva sin re-autenticarse aunque cierre el navegador.
     */
    private Boolean rememberMe = false;
}