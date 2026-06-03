package com.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DeleteProfileResponseDTO es un DTO (Data Transfer Object) que representa la respuesta de eliminación de perfil para un usuario autenticado. Contiene los campos necesarios para validar y procesar la respuesta de eliminación de perfil.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteProfileResponseDTO {

    /** Indica si el perfil fue eliminado */
    private Boolean deleted;
    /** El tipo de eliminación */
    private String deletionType;
    /** El ID de la operación */
    private String operationId;
    /** La fecha y hora de la eliminación */
    private LocalDateTime timestamp;
    
}