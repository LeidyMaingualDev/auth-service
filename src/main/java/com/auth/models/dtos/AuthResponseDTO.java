package com.auth.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * DTO de respuesta devuelto al cliente tras una autenticación exitosa.
 *
 * <p>Contiene los tokens JWT necesarios para que el cliente mantenga la sesión,
 * junto con datos básicos del usuario para personalizar la interfaz sin
 * necesidad de llamadas adicionales al servidor.</p>
 *
 * <p>Este DTO se usa como payload en el campo {@code data} de {@link ApiResponseDTO}
 * en los endpoints de registro, login y renovación de token.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    /**
     * Token de acceso JWT de corta duración (configurado en {@code jwt.expiration}).
     * Debe incluirse en la cabecera {@code Authorization: Bearer <token>} en cada petición.
     */
    private String token;

    /**
     * Token de refresco JWT de larga duración (configurado en {@code jwt.refresh-expiration}).
     * Se usa exclusivamente en el endpoint {@code POST /auth/refresh-token} para obtener
     * un nuevo access token sin re-autenticarse.
     */
    private String refreshToken;

    private Long userId;



    /** Correo electrónico del usuario autenticado. */
    private String email;

    /** Nombre de pila del usuario autenticado, para personalización del frontend. */
    private String name;

    /** Rol principal del usuario (ej. {@code "USER"}, {@code "ADMIN"}). */
    private String role;
}