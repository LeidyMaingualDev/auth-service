package com.auth.models.enums;

/**
 * Enumeración de los roles del sistema Qvenly.
 * Solo existen dos roles: ADMIN y USER.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
public enum SystemRole {

    /** Rol de administrador — acceso total al sistema. */
    ADMIN("ADMIN"),

    /** Rol de usuario base — asignado automáticamente en el registro. */
    USER("USER");

    private final String value;

    SystemRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}