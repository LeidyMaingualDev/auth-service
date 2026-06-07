package com.auth.repositories;

import com.auth.models.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;


/**
 * Repositorio JPA para la entidad {@link LoginAttempt}.
 *
 * <p>Proporciona consultas para detectar actividad sospechosa de inicio de sesión,
 * como múltiples intentos fallidos desde una misma cuenta en un período de tiempo.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see LoginAttempt
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Cuenta los intentos de inicio de sesión por correo, resultado y ventana de tiempo.
     *
     * <p>Se usa para detectar ataques de fuerza bruta. Ejemplo de uso para contar
     * intentos fallidos en los últimos 15 minutos:</p>
     * <pre>{@code
     * long count = loginAttemptRepository.countByEmailAndSuccessAndAttemptedAtAfter(
     *     email, false, LocalDateTime.now().minusMinutes(15)
     * );
     * }</pre>
     *
     * @param email    correo electrónico del usuario
     * @param success  {@code false} para contar intentos fallidos; {@code true} para exitosos
     * @param after    límite inferior de tiempo (solo se cuentan intentos posteriores a esta fecha)
     * @return número de intentos que coinciden con los criterios
     */
    long countByEmailAndSuccessAndAttemptedAtAfter(
            String email, boolean success, LocalDateTime after
    );

    /**
     * Obtiene los intentos de inicio de sesión de un usuario ordenados por fecha descendente.
     *
     * <p>Útil para auditoría o para mostrar el historial de accesos en un panel de administración.</p>
     *
     * @param email   correo electrónico del usuario
     * @param success {@code false} para obtener solo los fallidos; {@code true} para los exitosos
     * @return lista de intentos ordenada del más reciente al más antiguo
     */
    List<LoginAttempt> findByEmailAndSuccessOrderByAttemptedAtDesc(
            String email, boolean success
    );
}