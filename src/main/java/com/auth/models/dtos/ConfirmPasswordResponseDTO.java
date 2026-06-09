package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ConfirmPasswordResponseDTO es un DTO (Data Transfer Object) que representa la respuesta de confirmación de contraseña para un usuario autenticado. Contiene los campos necesarios para validar y procesar la respuesta de confirmación de contraseña, incluyendo la validez de la contraseña y la fecha de validación.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPasswordResponseDTO {

    /** La validez de la contraseña */
    private Boolean valid;
    /** La fecha de validación */
    private LocalDateTime validatedAt;
}