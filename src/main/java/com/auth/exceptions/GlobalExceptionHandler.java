package com.auth.exceptions;

import com.auth.models.dtos.ApiResponseDTO;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el microservicio de autenticación.
 *
 * <p>Intercepta excepciones no manejadas en los controladores y las convierte en
 * respuestas HTTP estructuradas usando {@link ApiResponseDTO}, garantizando que
 * el cliente siempre reciba un formato de error consistente.</p>
 *
 * <p>Excepciones manejadas actualmente:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — errores de validación de Bean Validation
 *       ({@code @Valid}), devuelve {@code 400 Bad Request} con el mapa de campos y mensajes.</li>
 *   <li>{@link RuntimeException} — cualquier excepción de tiempo de ejecución no prevista,
 *       devuelve {@code 500 Internal Server Error} con un mensaje genérico.</li>
 * </ul>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see ApiResponseDTO
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de campos generados por {@code @Valid} en los controladores.
     *
     * <p>Recopila todos los errores de campo del resultado de binding y los organiza
     * en un mapa {@code campo → mensaje}, que se devuelve al cliente para facilitar
     * la presentación de errores por campo en el frontend.</p>
     *
     * <p>En caso de múltiples errores para el mismo campo, se conserva el primer
     * mensaje encontrado.</p>
     *
     * @param ex excepción lanzada por Spring MVC cuando la validación falla
     * @return {@code 400 Bad Request} con un mapa de errores por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.<Map<String, String>>builder()
                .success(false)
                .message("Error de validación en los campos enviados")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja cualquier {@link RuntimeException} no capturada explícitamente.
     *
     * <p>Actúa como salvaguarda de último recurso. Devuelve un mensaje genérico
     * sin exponer detalles internos del sistema al cliente.</p>
     *
     * @param ex excepción de tiempo de ejecución no manejada
     * @return {@code 500 Internal Server Error} con mensaje genérico
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error("Error interno del servidor"));
    }
}