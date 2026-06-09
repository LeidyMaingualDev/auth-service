package com.auth.repositories;

import com.auth.models.entities.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la configuracion de notificaciones del usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Busca la configuracion de notificaciones asociada a un usuario.
     *
     * @param userId identificador del usuario
     * @return preferencia encontrada o vacia si aun no existe
     */
    Optional<NotificationPreference> findByUserId(Long userId);
}