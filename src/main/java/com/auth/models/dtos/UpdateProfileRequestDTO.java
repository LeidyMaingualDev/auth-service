package com.auth.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de solicitud para actualizar los datos del perfil del usuario autenticado.
 *
 * <p>Los campos {@code name}, {@code lastName} y {@code email} son obligatorios.
 * Los campos {@code phoneNumber}, {@code documentType} y {@code documentNumber}
 * son opcionales — un usuario puede actualizar su nombre sin tener documento registrado.</p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {

    /** Nombre del usuario. Obligatorio, máximo 100 caracteres. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    /** Apellido del usuario. Obligatorio, máximo 100 caracteres. */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String lastName;

    /** Correo electrónico del usuario. Obligatorio y debe tener formato válido. */
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El formato del correo electronico no es valido")
    @Size(max = 100, message = "El correo electronico no puede superar 100 caracteres")
    private String email;

    /**
     * Número de teléfono del usuario. Opcional.
     * Formato internacional con {@code +}. Entre 7 y 20 dígitos.
     */
    @Pattern(
            regexp = "^\\+?[0-9]{7,20}$",
            message = "El numero de telefono no es valido"
    )
    private String phoneNumber;

    /** Tipo de documento del usuario. Opcional. Valores: CC, CE, PASSPORT, TI. */
    private String documentType;

    /** Número de documento del usuario. Opcional, máximo 16 caracteres. */
    @Size(max = 16, message = "El numero de documento no puede superar 16 caracteres")
    private String documentNumber;
}