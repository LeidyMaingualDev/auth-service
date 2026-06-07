package com.auth.models.dtos;

import com.auth.models.enums.DocumentType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de solicitud para el registro de un nuevo usuario.
 *
 * <p>Aplica validaciones de Bean Validation ({@code @Valid}) para garantizar
 * la integridad de los datos antes de llegar a la capa de servicio. Los campos
 * {@code documentNumber}, {@code documentType} y {@code phoneNumber} son opcionales.</p>
 *
 * <p>Restricciones de contraseña: mínimo 8 caracteres, al menos una mayúscula,
 * una minúscula, un dígito y un carácter especial ({@code @$!%*?&}).</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    /** Nombre de pila. Entre 2 y 50 caracteres. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String name;

    /** Apellido. Entre 2 y 50 caracteres. */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    /** Correo electrónico. Debe ser único en el sistema. */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    /**
     * Contraseña. Mínimo 8 caracteres con al menos una mayúscula, una minúscula,
     * un dígito y un carácter especial {@code @$!%*?&}.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_])[A-Za-z\\d\\W_]+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;

    /** Número de documento de identidad. Entre 6 y 15 dígitos. Opcional. */
    @Pattern(
            regexp = "^[0-9]{6,15}$",
            message = "El número de documento debe contener entre 6 y 15 dígitos"
    )
    private String documentNumber;

    /** Tipo de documento de identidad. Opcional. Ver {@link DocumentType}. */
    private DocumentType documentType;

    /** Número de teléfono. Formato internacional opcional con {@code +}. Entre 7 y 15 dígitos. */
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "El número de teléfono no es válido"
    )
    private String phoneNumber;
}