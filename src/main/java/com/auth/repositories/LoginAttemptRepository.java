package com.auth.repositories;

import com.auth.models.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link LoginAttempt}.
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see LoginAttempt
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Cuenta los intentos de inicio de sesion por correo, resultado y ventana de tiempo.
     *
     * @param email correo electronico del usuario
     * @param success resultado del intento
     * @param after limite inferior de tiempo
     * @return numero de intentos que coinciden con los criterios
     */
    long countByEmailAndSuccessAndAttemptedAtAfter(
            String email, boolean success, LocalDateTime after
    );

    /**
     * Obtiene los intentos de inicio de sesion de un usuario ordenados por fecha descendente.
     *
     * @param email correo electronico del usuario
     * @param success resultado del intento
     * @return lista de intentos ordenada del mas reciente al mas antiguo
     */
    List<LoginAttempt> findByEmailAndSuccessOrderByAttemptedAtDesc(
            String email, boolean success
    );

    /**
     * Obtiene el ultimo intento de inicio de sesion exitoso para un correo.
     *
     * @param email correo electronico del usuario
     * @param success resultado del intento
     * @return intento mas reciente si existe
     */
    Optional<LoginAttempt> findFirstByEmailAndSuccessOrderByAttemptedAtDesc(
            String email, boolean success
    );
}