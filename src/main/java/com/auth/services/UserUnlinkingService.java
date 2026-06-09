package com.auth.services;

import com.auth.models.entities.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de preparar la desvinculacion local del usuario dentro del
 * Auth Service.
 *
 * <p>
 * La desvinculacion completa con actividades, eventos u otros datos externos
 * debe ser coordinada con los microservicios propietarios de esa informacion.
 * Este servicio limpia las relaciones locales que pertenecen al microservicio
 * de autenticacion antes de ejecutar la eliminacion del usuario.
 * </p>
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Service
public class UserUnlinkingService {

    /**
     * Elimina asociaciones locales del usuario antes de su eliminacion
     * definitiva.
     *
     * @param user usuario autenticado que sera desvinculado
     */
    @Transactional
    public void unlinkLocalUserRelations(User user) {
        if (user.getRoles() != null) {
            user.getRoles().clear();
        }
    }
}