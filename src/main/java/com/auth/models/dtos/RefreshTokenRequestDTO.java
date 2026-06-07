package com.auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO de solicitud para renovar el access token usando un refresh token válido.
 *
 * <p>Se usa en el endpoint {@code POST /auth/refresh-token}. El cliente debe
 * enviar el refresh token obtenido durante el login o el último refresh exitoso.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    /** Refresh token JWT de larga duración previamente emitido por el servidor. */
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}