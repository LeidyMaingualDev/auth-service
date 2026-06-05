package com.auth.repositories;

import com.auth.models.entities.NotificationInbox;
import com.auth.models.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para consultar y actualizar la bandeja de notificaciones del
 * usuario.
 *
 * @author Natali Ramirez
 * @version 1.0
 */
@Repository
public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    /**
     * Obtiene todas las notificaciones de un usuario ordenadas desde la mas
     * reciente.
     *
     * @param userId identificador del usuario
     * @return lista de notificaciones del usuario
     */
    List<NotificationInbox> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Obtiene las notificaciones no leidas de un usuario.
     *
     * @param userId identificador del usuario
     * @return lista de notificaciones pendientes de lectura
     */
    List<NotificationInbox> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Busca notificaciones con envio externo fallido que aun pueden reintentarse.
     *
     * @param status     estado de la notificacion
     * @param retryCount numero maximo de reintentos no superado
     * @return notificaciones candidatas para reintento
     */
    List<NotificationInbox> findByStatusAndRetryCountLessThan(
            NotificationStatus status,
            Integer retryCount);
}
