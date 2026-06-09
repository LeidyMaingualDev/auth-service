package com.auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la solicitud segura para eliminar el perfil del usuario.
 *
 * <p>
 * La contrasena se solicita para confirmar que la eliminacion fue iniciada por
 * el titular autenticado de la cuenta.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteProfileRequestDTO {

    /** Contrasena actual del usuario para confirmar la eliminacion. */
    @NotBlank(message = "La contrasena es obligatoria para eliminar el perfil")
    private String password;
}