package com.auth.models.enums;

/**
 * Enumeración de los tipos de documento de identidad aceptados en el sistema.
 *
 * <p>Se persiste como cadena de texto ({@code @Enumerated(EnumType.STRING)})
 * en la columna {@code document_type} de la tabla {@code users}.</p>
 *
 * <ul>
 *   <li>{@link #CC}       — Cédula de Ciudadanía (Colombia)</li>
 *   <li>{@link #CE}       — Cédula de Extranjería (Colombia)</li>
 *   <li>{@link #PASSPORT} — Pasaporte internacional</li>
 *   <li>{@link #TI}       — Tarjeta de Identidad (Colombia, menores de edad)</li>
 * </ul>
 *
 * @author Equipo Qvenly
 * @version Leidy Martinez
 */

public enum DocumentType {
    /** Cédula de Ciudadanía colombiana. */
    CC,

    /** Cédula de Extranjería colombiana. */
    CE,

    /** Pasaporte internacional. */
    PASSPORT,

    /** Tarjeta de Identidad para menores de edad en Colombia. */
    TI
}