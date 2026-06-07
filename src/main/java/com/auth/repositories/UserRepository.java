package com.auth.repositories;

import com.auth.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link User}.
 *
 * <p>Proporciona las operaciones CRUD heredadas de {@link JpaRepository}
 * más consultas derivadas específicas para la autenticación, el registro
 * y el flujo de Google OAuth2.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * <p>Usado por {@code UserDetailsServiceImpl} para cargar el usuario
     * durante la autenticación, y por {@code AuthService} para verificar
     * credenciales y recuperar datos del usuario.</p>
     *
     * @param email correo electrónico a buscar
     * @return {@link Optional} con el usuario si existe, o vacío si no
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario con el correo electrónico dado.
     *
     * <p>Usado en el registro para evitar duplicados antes de persistir el usuario.</p>
     *
     * @param email correo electrónico a verificar
     * @return {@code true} si el correo ya está registrado; {@code false} en caso contrario
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si ya existe un usuario con el número de documento dado.
     *
     * <p>Usado en el registro para garantizar la unicidad del documento de identidad.</p>
     *
     * @param documentNumber número de documento a verificar
     * @return {@code true} si el documento ya está registrado; {@code false} en caso contrario
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Busca un usuario por su token de verificación de correo electrónico.
     *
     * <p>Usado en el flujo de confirmación de cuenta. El token es un UUID de un solo
     * uso generado durante el registro y eliminado tras la confirmación exitosa.</p>
     *
     * @param verificationToken token UUID enviado al correo del usuario
     * @return {@link Optional} con el usuario si el token existe, o vacío si no
     */
    Optional<User> findByVerificationToken(String verificationToken);

    /**
     * Busca un usuario por su identificador único de Google (campo {@code sub}).
     *
     * <p>Usado en el flujo de Google OAuth2 para identificar usuarios existentes
     * de forma inequívoca sin depender del correo electrónico, que en casos
     * excepcionales puede cambiar en Google.</p>
     *
     * @param googleId identificador único de Google ({@code sub} del perfil OAuth2)
     * @return {@link Optional} con el usuario si existe, o vacío si es un usuario nuevo
     */
    Optional<User> findByGoogleId(String googleId);
}