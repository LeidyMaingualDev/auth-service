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
 * <p>Los tokens viajan en este DTO hasta el Gateway, donde el filtro
 * {@code AuthResponseCookieFilter} los extrae, los convierte en cookies
 * {@code HttpOnly} y los elimina del body antes de enviarlo al cliente.
 * De esta forma, el frontend nunca accede directamente al valor del token.</p>
 *
 * <p>Este DTO se usa como payload en el campo {@code data} de {@link ApiResponseDTO}
 * en los endpoints de registro, login y renovación de token.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see ApiResponseDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    /**
     * Token de acceso JWT de corta duración (15 minutos por defecto).
     * El Gateway lo convierte en cookie {@code HttpOnly} antes de enviarlo al cliente.
     * Contiene los claims {@code role} y {@code userId} para autorización sin consultas adicionales.
     */
    private String token;

    /**
     * Token de refresco JWT de larga duración.
     * Duración: 1 día sin "Recuérdame", 7 días con "Recuérdame".
     * El Gateway lo convierte en cookie {@code HttpOnly}.
     * Se usa exclusivamente en {@code POST /auth/refresh-from-cookie}
     * para obtener un nuevo access token sin re-autenticarse.
     */
    private String refreshToken;

    /**
     * Identificador único del usuario autenticado.
     * Usado por el frontend para construir llamadas a otros microservicios
     * que requieren el ID del usuario (ej. consultar plan activo).
     */
    private Long userId;

    /** Correo electrónico del usuario autenticado. */
    private String email;

    /** Nombre de pila del usuario autenticado, para personalización del frontend. */
    private String name;

    /** Rol principal del usuario (ej. {@code "USER"}, {@code "ADMIN"}). */
    private String role;
}