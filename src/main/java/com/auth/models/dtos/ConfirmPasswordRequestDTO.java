package com.auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ConfirmPasswordRequestDTO es un DTO (Data Transfer Object) que representa la solicitud de confirmación de contraseña para un usuario autenticado. Contiene el campo necesario para validar y procesar la solicitud de confirmación de contraseña, que es la contraseña a confirmar.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPasswordRequestDTO {

    /** La contraseña a confirmar */
    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}