package com.auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChangePasswordRequestDTO es un DTO (Data Transfer Object) que representa la solicitud de cambio de contraseña para un usuario autenticado. Contiene los campos necesarios para validar y procesar la solicitud de cambio de contraseña, incluyendo la contraseña actual, la nueva contraseña y la confirmación de la nueva contraseña.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {

    /** La contraseña actual del usuario */
    @NotBlank(message = "La contrasena actual es obligatoria")
    private String currentPassword;

    /** La nueva contraseña del usuario */
    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener minimo 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "La contrasena debe tener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    private String newPassword;

    /** La confirmación de la nueva contraseña */
    @NotBlank(message = "La confirmacion de contrasena es obligatoria")
    private String confirmPassword;
}