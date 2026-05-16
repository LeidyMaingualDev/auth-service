package com.auth.repositories;

import com.auth.models.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Role}.
 *
 * <p>Se utiliza principalmente durante el registro de usuarios para asignar
 * el rol {@code "USER"} por defecto al nuevo usuario.</p>
 *
 * @author Leidy Martinez
 * @version 1.0
 * @see Role
 */

@Repository
public interface RoleRepository extends JpaRepository <Role, Integer> {

    /**
     * Busca un rol por su nombre exacto.
     *
     * <p>Ejemplo de uso: {@code roleRepository.findByName("USER")}.</p>
     *
     * @param name nombre del rol (ej. {@code "USER"}, {@code "ADMIN"})
     * @return {@link Optional} con el rol si existe, o vacío si no
     */
    Optional<Role> findByName(String name);
}
