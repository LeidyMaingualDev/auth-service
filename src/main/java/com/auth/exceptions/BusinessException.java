package com.auth.exceptions;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * Excepción de negocio genérica que encapsula un mensaje de error y un código HTTP.
 *
 * <p>Permite lanzar errores semánticos desde la capa de servicio sin acoplarse a
 * la capa HTTP. El {@link GlobalExceptionHandler} puede capturarla y devolver la
 * respuesta adecuada al cliente.</p>
 *
 * <p>Se proporcionan métodos de fábrica estáticos para los casos de uso más comunes,
 * lo que hace el código del servicio más expresivo:</p>
 * <pre>{@code
 * throw BusinessException.conflict("El correo ya está registrado");
 * throw BusinessException.notFound("Usuario no encontrado");
 * throw BusinessException.unauthorized("Credenciales inválidas");
 * throw BusinessException.badRequest("La contraseña no cumple los requisitos");
 * }</pre>
 *
 * @author Leidy Martinez
 * @version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /** Código de estado HTTP asociado al error de negocio. */
    private final HttpStatus status;

    /**
     * Construye una excepción de negocio con mensaje y código HTTP personalizados.
     *
     * @param message descripción del error que se devolverá al cliente
     * @param status  código HTTP que debe usar la respuesta
     */
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Crea una excepción de conflicto ({@code 409 Conflict}).
     *
     * <p>Útil cuando se intenta crear un recurso que ya existe, por ejemplo
     * registrar un correo electrónico duplicado.</p>
     *
     * @param message descripción del conflicto
     * @return excepción con estado {@link HttpStatus#CONFLICT}
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }

    /**
     * Crea una excepción de recurso no encontrado ({@code 404 Not Found}).
     *
     * @param message descripción del recurso que no fue encontrado
     * @return excepción con estado {@link HttpStatus#NOT_FOUND}
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Crea una excepción de acceso no autorizado ({@code 401 Unauthorized}).
     *
     * <p>Se usa cuando las credenciales son inválidas o el token ha expirado.</p>
     *
     * @param message descripción del error de autenticación
     * @return excepción con estado {@link HttpStatus#UNAUTHORIZED}
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Crea una excepción de solicitud incorrecta ({@code 400 Bad Request}).
     *
     * <p>Se usa cuando los datos de entrada no son válidos o no cumplen
     * las reglas de negocio esperadas.</p>
     *
     * @param message descripción del error en los datos de entrada
     * @return excepción con estado {@link HttpStatus#BAD_REQUEST}
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}