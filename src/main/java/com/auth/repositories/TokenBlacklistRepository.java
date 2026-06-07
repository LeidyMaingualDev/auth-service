package com.auth.repositories;

import com.auth.models.entities.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad {@link TokenBlacklist}.
 *
 * <p>Permite verificar rápidamente si un token JWT ha sido revocado,
 * lo que es consultado en cada petición HTTP por el filtro {@code JwtAuthFilter}.</p>
 *
 * <p><strong>Consideración de rendimiento:</strong> dado que esta consulta se ejecuta
 * en cada request autenticado, se recomienda agregar un índice en la columna {@code token}
 * de la tabla {@code token_blacklist} en producción.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see TokenBlacklist
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    /**
     * Verifica si un token JWT está registrado en la blacklist.
     *
     * @param token valor del token JWT a verificar
     * @return {@code true} si el token fue revocado; {@code false} si es válido
     */
    boolean existsByToken(String token);
}