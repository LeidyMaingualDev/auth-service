package com.auth.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de solicitud para restablecer la contraseña usando el token de recuperación.
 *
 * <p>El token UUID es recibido como query param en el frontend y enviado en este DTO
 * junto con la nueva contraseña y su confirmación. La validación de que ambas
 * contraseñas coincidan se realiza en la capa de servicio.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    /** Token UUID generado durante el flujo {@code forgot-password}. */
    @NotBlank(message = "El token es obligatorio")
    private String token;

    /**
     * Nueva contraseña. Debe cumplir los mismos requisitos que en el registro:
     * mínimo 8 caracteres, mayúscula, minúscula, dígito y carácter especial.
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_])[A-Za-z\\d\\W_]+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String newPassword;

    /** Confirmación de la nueva contraseña. Debe ser idéntica a {@code newPassword}. */
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmPassword;
}