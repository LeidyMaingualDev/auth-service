package com.auth.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateProfileRequestDTO es un DTO (Data Transfer Object) que representa la solicitud de actualización de perfil para un usuario autenticado. Contiene los campos necesarios para validar y procesar la solicitud de actualización de perfil.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {

    /** El nombre del usuario */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    /** El apellido del usuario */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String lastName;

    /** El correo electrónico del usuario */
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El formato del correo electronico no es valido")
    @Size(max = 100, message = "El correo electronico no puede superar 100 caracteres")
    private String email;

    /** El número de teléfono del usuario */
    @NotBlank(message = "El numero de telefono es obligatorio")
    @Size(max = 20, message = "El numero de telefono no puede superar 20 caracteres")
    private String phoneNumber;

    /** El tipo de documento del usuario */
    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType;

    /** El número de documento del usuario */
    @NotBlank(message = "El numero de documento es obligatorio")
    @Size(max = 16, message = "El numero de documento no puede superar 16 caracteres")
    private String documentNumber;
}
