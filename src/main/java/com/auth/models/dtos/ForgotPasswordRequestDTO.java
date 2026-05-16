package com.auth.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de solicitud para iniciar el flujo de recuperación de contraseña.
 *
 * <p>El usuario proporciona su correo electrónico y el sistema envía un enlace
 * de restablecimiento si el correo está registrado. Por seguridad, la respuesta
 * es siempre la misma independientemente de si el correo existe.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDTO {

    /** Correo electrónico al que se enviará el enlace de recuperación. */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;
}