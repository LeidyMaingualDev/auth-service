package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para mostrar informacion real de la sesion actual del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSecurityResponseDTO {

    /** Dispositivo o navegador detectado desde el User-Agent. */
    private String device;

    /** IP asociada al ultimo inicio de sesion exitoso o a la solicitud actual. */
    private String ipAddress;

    /** Fecha y hora del ultimo inicio de sesion exitoso. */
    private LocalDateTime startedAt;
}