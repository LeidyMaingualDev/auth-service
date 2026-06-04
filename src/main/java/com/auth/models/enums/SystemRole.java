package com.auth.models.enums;

/**
 * Enumeración que representa los roles del sistema.
 */
public enum SystemRole {
    /** Rol de administrador */
    ADMIN ("ADMIN"),

    /** Rol de organizador */
    ORGANIZER ("ORGANIZER"),

    /** Rol de usuario */
    USER ("USER");

    private final String value;

    /**
     * Constructor de la enumeración.
     * @param value el valor del rol
     */
    SystemRole(String value) {
        this.value = value;
    }

    /**
     * Obtiene el valor del rol.
     * @return el valor del rol
     */
    public String getValue() {
        return value;
    }
}
