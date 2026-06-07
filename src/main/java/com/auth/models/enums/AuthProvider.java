package com.auth.models.enums;

/**
 * Enumeración de los proveedores de autenticación soportados por el sistema Qvenly.
 *
 * <p>Determina el método con el que un usuario creó su cuenta y define
 * cómo debe autenticarse en adelante. El sistema aplica la política de
 * <b>no mezclar proveedores</b>: un usuario registrado con {@code LOCAL}
 * solo puede iniciar sesión con correo y contraseña, y un usuario registrado
 * con {@code GOOGLE} solo puede hacerlo mediante Google OAuth2.</p>
 *
 * <p>Se persiste como cadena de texto ({@code @Enumerated(EnumType.STRING)})
 * en la columna {@code auth_provider} de la tabla {@code users}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see com.auth.models.entities.User
 */
public enum AuthProvider {

    /**
     * Registro e inicio de sesión con correo electrónico y contraseña propia del sistema.
     * La contraseña se almacena con hash BCrypt en la base de datos.
     */
    LOCAL,

    /**
     * Registro e inicio de sesión mediante Google OAuth2.
     * La contraseña del usuario es {@code null} — la autenticación
     * la gestiona exclusivamente Google.
     */
    GOOGLE
}