package com.auth.security.roles;

import com.auth.models.enums.SystemRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación personalizada para requerir roles específicos en métodos o clases. Permite especificar uno o más roles del sistema que son necesarios para acceder a un recurso protegido.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredRole {

    /**
     * Obtiene los roles requeridos.
     * @return los roles requeridos
     */
    SystemRole[] value();
}
