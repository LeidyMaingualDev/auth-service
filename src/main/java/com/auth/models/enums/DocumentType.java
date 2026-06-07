package com.auth.models.enums;

/**
 * Enumeración de los tipos de documento de identidad aceptados en el sistema Qvenly.
 *
 * <p>Se persiste como cadena de texto ({@code @Enumerated(EnumType.STRING)})
 * en la columna {@code document_type} de la tabla {@code users}.</p>
 *
 * <p>Este campo es opcional para usuarios registrados mediante Google OAuth2
 * y obligatorio para usuarios registrados con correo y contraseña.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see com.auth.models.entities.User
 */
public enum DocumentType {

    /** Cédula de Ciudadanía colombiana. Documento principal para ciudadanos mayores de edad. */
    CC,

    /** Cédula de Extranjería colombiana. Documento para residentes extranjeros en Colombia. */
    CE,

    /** Pasaporte internacional. Válido para usuarios de cualquier nacionalidad. */
    PASSPORT,

    /** Tarjeta de Identidad colombiana. Documento para menores de edad en Colombia. */
    TI
}