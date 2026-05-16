package com.auth.repositories;

import com.auth.models.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link PasswordResetToken}.
 *
 * <p>Gestiona los tokens UUID de un solo uso para la recuperación de contraseña.
 * Incluye una operación de actualización masiva para invalidar tokens anteriores
 * del mismo usuario antes de emitir uno nuevo.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see PasswordResetToken
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token de recuperación por su valor UUID.
     *
     * @param token valor UUID del token
     * @return {@link Optional} con el token si existe y no ha sido eliminado, o vacío si no
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalida todos los tokens de recuperación activos de un usuario marcándolos como usados.
     *
     * <p>Se invoca antes de crear un nuevo token para garantizar que solo el token
     * más reciente sea válido, evitando el uso de tokens de solicitudes anteriores.</p>
     *
     * @param userId identificador del usuario cuyos tokens deben invalidarse
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user.id = :userId AND p.used = false")
    void invalidatePreviousTokens(Long userId);
}