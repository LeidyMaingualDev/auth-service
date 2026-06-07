package com.auth.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Envoltorio genérico de respuesta HTTP para todos los endpoints del microservicio.
 *
 * <p>Estandariza la estructura de cada respuesta JSON asegurando que el cliente
 * siempre reciba los campos {@code success}, {@code message} y {@code timestamp},
 * más el campo opcional {@code data} que puede contener cualquier tipo de carga útil.</p>
 *
 * <p>Los campos con valor {@code null} se omiten en la serialización JSON gracias a
 * {@code @JsonInclude(NON_NULL)}, reduciendo el tamaño de la respuesta.</p>
 *
 * <p>Uso típico en la capa de servicio:</p>
 * <pre>{@code
 * // Respuesta exitosa con datos
 * return ApiResponseDTO.ok("Usuario registrado exitosamente", authResponseDTO);
 *
 * // Respuesta exitosa sin datos
 * return ApiResponseDTO.ok("Correo enviado");
 *
 * // Respuesta de error
 * return ApiResponseDTO.error("El correo ya está registrado");
 * }</pre>
 *
 * @param <T> tipo de datos contenidos en el campo {@code data}
 * @author Leidy Martinez
 * @version 3.0
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    /** Indica si la operación fue exitosa ({@code true}) o falló ({@code false}). */
    private boolean success;

    /** Mensaje descriptivo del resultado, tanto para éxito como para error. */
    private String message;

    /** Carga útil de la respuesta; {@code null} si la operación no retorna datos. */
    private T data;

    /** Marca de tiempo del momento en que se generó la respuesta. */
    private LocalDateTime timestamp;

    /**
     * Crea una respuesta exitosa con datos.
     *
     * @param <T>     tipo de los datos
     * @param message mensaje descriptivo del éxito
     * @param data    datos a incluir en la respuesta
     * @return instancia con {@code success = true}, el mensaje y los datos
     */
    public static <T> ApiResponseDTO<T> ok(String message, T data) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea una respuesta exitosa sin datos.
     *
     * @param <T>     tipo genérico (generalmente {@code Void})
     * @param message mensaje descriptivo del éxito
     * @return instancia con {@code success = true} y sin campo {@code data}
     */
    public static <T> ApiResponseDTO<T> ok(String message) {
        return ok(message, null);
    }

    /**
     * Crea una respuesta de error sin datos adicionales.
     *
     * @param <T>     tipo genérico (generalmente {@code Void})
     * @param message descripción del error ocurrido
     * @return instancia con {@code success = false} y el mensaje de error
     */
    public static <T> ApiResponseDTO<T> error(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}