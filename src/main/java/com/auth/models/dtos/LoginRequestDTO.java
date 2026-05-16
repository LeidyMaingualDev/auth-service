package com.auth.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de solicitud para el inicio de sesión de un usuario existente.
 *
 * <p>Contiene las credenciales mínimas requeridas para autenticarse, más la opción
 * de mantener la sesión activa mediante {@code rememberMe}. Cuando {@code rememberMe}
 * es {@code true}, el token generado tiene la duración del refresh token
 * ({@code jwt.refresh-expiration}) en lugar del token de acceso estándar.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    /** Correo electrónico del usuario. Debe tener formato válido de email. */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    /** Contraseña del usuario. No se valida el formato (ya fue validado en el registro). */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Indica si el usuario desea mantener la sesión activa por más tiempo.
     * Por defecto {@code false}; cuando es {@code true} extiende la duración del token.
     */
    private Boolean rememberMe = false;
}