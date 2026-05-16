package com.auth.models.entities;

import com.auth.models.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entidad JPA que representa un usuario registrado en el sistema.
 *
 * <p>Implementa {@link UserDetails} de Spring Security, lo que permite que esta
 * entidad sea usada directamente por el framework para la autenticación y
 * autorización, sin capas intermedias de conversión.</p>
 *
 * <p>Los roles se cargan con {@code FetchType.EAGER} para que estén disponibles
 * en el momento en que Spring Security construye el contexto de seguridad,
 * evitando {@code LazyInitializationException} fuera de una transacción activa.</p>
 *
 * <p>Tabla en base de datos: {@code users}</p>
 *
 * @author Equipo Qvenly
 * @version Leidy Martinez
 * @see Role
 * @see UserDetails
 */

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /** Identificador único generado automáticamente por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de pila del usuario. No puede ser nulo. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Apellido del usuario. No puede ser nulo. */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** Correo electrónico. Único en el sistema y usado como nombre de usuario para login. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Contraseña almacenada con hash BCrypt. Nunca se devuelve en respuestas HTTP. */
    @Column(name = "password", nullable = false)
    private String password;

    /** Número de documento de identidad. Opcional y único si se proporciona. */
    @Column(name = "document_number", unique = true)
    private String documentNumber;

    /**
     * Tipo de documento de identidad. Se persiste como cadena (ej. {@code "CC"}).
     *
     * @see DocumentType
     */
    @Enumerated(EnumType.STRING)  // mapea el ENUM de la BD
    @Column(name = "document_type")
    private DocumentType documentType;

    /** Número de teléfono del usuario. Opcional. */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Indica si la cuenta está activa. Las cuentas inactivas no pueden iniciar sesión.
     * Valor por defecto: {@code true}.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Fecha y hora de creación del registro. Se asigna automáticamente en {@link #onCreate()}. */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Conjunto de roles asignados al usuario. Se carga de forma eagerly para
     * que Spring Security pueda leer las autoridades en cualquier punto del ciclo de vida.
     *
     * <p>Tabla de unión: {@code user_roles} con columnas {@code user_id} y {@code role_id}.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)  // EAGER para cargar roles
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;


    /**
     * Convierte los roles del usuario en autoridades de Spring Security.
     *
     * <p>Cada rol se prefija con {@code ROLE_} siguiendo la convención de Spring Security
     * (ej. el rol {@code "ADMIN"} se convierte en la autoridad {@code "ROLE_ADMIN"}).</p>
     *
     * @return colección de {@link GrantedAuthority} basada en los roles del usuario
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()                  // ← lee los roles reales de la BD
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el correo electrónico como nombre de usuario para Spring Security.
     *
     * @return correo electrónico del usuario
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * La caducidad de cuenta no está implementada. Siempre devuelve {@code true}.
     *
     * @return {@code true} — la cuenta nunca expira
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * El bloqueo de cuenta no está implementado. Siempre devuelve {@code true}.
     *
     * @return {@code true} — la cuenta nunca se bloquea automáticamente
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * La caducidad de credenciales no está implementada. Siempre devuelve {@code true}.
     *
     * @return {@code true} — las credenciales nunca expiran
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Devuelve si la cuenta está habilitada, basándose en el campo {@code isActive}.
     *
     * @return {@code true} si la cuenta está activa; {@code false} si fue desactivada
     */
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    /**
     * Hook de JPA que asigna la fecha de creación antes del primer {@code INSERT}.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}