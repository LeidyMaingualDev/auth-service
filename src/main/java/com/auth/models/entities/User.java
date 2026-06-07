package com.auth.models.entities;

import com.auth.models.enums.AuthProvider;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entidad que representa un usuario registrado en el sistema Qvenly.
 *
 * <p>Implementa {@link UserDetails} para integrarse con Spring Security,
 * permitiendo que el framework gestione la autenticación y autorización
 * directamente a partir de esta entidad.</p>
 *
 * <p>Soporta dos proveedores de autenticación:</p>
 * <ul>
 *   <li><b>LOCAL</b> — registro con correo electrónico y contraseña.</li>
 *   <li><b>GOOGLE</b> — registro e inicio de sesión mediante OAuth2 con Google.
 *       En este caso la contraseña se almacena como {@code null} y el acceso
 *       se controla exclusivamente a través del proveedor externo.</li>
 * </ul>
 *
 * <p>Mapa de la tabla {@code users} en la base de datos {@code auth_and_user}.</p>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see Role
 * @see AuthProvider
 * @see DocumentType
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * Identificador único del usuario generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre(s) del usuario.
     * No puede ser nulo. Para usuarios de Google se obtiene del campo {@code given_name}.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Apellido(s) del usuario.
     * No puede ser nulo. Para usuarios de Google se obtiene del campo {@code family_name}.
     * Puede ser cadena vacía si Google no provee el apellido.
     */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /**
     * Correo electrónico del usuario. Actúa como identificador único de inicio de sesión.
     * No puede ser nulo ni repetirse entre usuarios.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Contraseña del usuario almacenada con hash BCrypt.
     * Es {@code null} para usuarios registrados con Google OAuth2,
     * ya que su autenticación no requiere contraseña propia del sistema.
     */
    @Column(name = "password", nullable = true)
    private String password;

    /**
     * Número de documento de identidad del usuario (cédula, pasaporte, etc.).
     * Es único en el sistema. Puede ser {@code null} para usuarios de Google
     * que no hayan completado su perfil.
     */
    @Column(name = "document_number", unique = true)
    private String documentNumber;

    /**
     * Tipo de documento de identidad del usuario.
     * Los valores posibles están definidos en {@link DocumentType}:
     * {@code CC}, {@code CE}, {@code PASSPORT}, {@code TI}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    /**
     * Número de teléfono de contacto del usuario.
     * Formato recomendado: {@code +573001234567}.
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * URL de la foto de perfil del usuario.
     * Para usuarios de Google, se obtiene del campo {@code picture} del perfil OAuth2
     * y se actualiza automáticamente si cambia en Google.
     * Para usuarios locales, puede ser {@code null}.
     */
    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    /**
     * Indica si la cuenta del usuario está activa y puede iniciar sesión.
     * Se establece en {@code true} tras confirmar el correo electrónico
     * o al registrarse mediante Google OAuth2.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    /**
     * Indica si el usuario ha verificado su correo electrónico.
     * Para usuarios locales, se establece en {@code true} al hacer clic
     * en el enlace de confirmación enviado por correo.
     * Para usuarios de Google, se establece según el campo {@code email_verified}
     * del perfil OAuth2.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * Token UUID de un solo uso para confirmar el correo electrónico.
     * Se genera durante el registro local y se elimina tras la confirmación exitosa.
     * Es {@code null} para usuarios de Google y para cuentas ya verificadas.
     */
    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    /**
     * Proveedor de autenticación con el que el usuario creó su cuenta.
     * <ul>
     *   <li>{@link AuthProvider#LOCAL} — registro con correo y contraseña.</li>
     *   <li>{@link AuthProvider#GOOGLE} — registro mediante Google OAuth2.</li>
     * </ul>
     * Una vez establecido, no se modifica. Impide que un usuario local
     * inicie sesión con Google y viceversa (política de no mezclar proveedores).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Identificador único de Google del usuario (campo {@code sub} del perfil OAuth2).
     * Permite identificar al usuario de Google de forma inequívoca sin depender
     * del correo, que puede cambiar en casos excepcionales.
     * Es {@code null} para usuarios locales.
     */
    @Column(name = "google_id", unique = true)
    private String googleId;

    /**
     * Fecha y hora de creación del registro del usuario.
     * Se establece automáticamente en {@link #onCreate()} y no puede modificarse.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última modificación del registro del usuario.
     * Se actualiza automáticamente en {@link #onUpdate()} cada vez que
     * se persiste un cambio en la entidad.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Conjunto de roles asignados al usuario.
     * Se carga de forma inmediata ({@code FetchType.EAGER}) para que Spring Security
     * pueda construir las autoridades sin necesidad de una sesión de base de datos abierta.
     * Los roles se gestionan a través de la tabla intermedia {@code user_roles}.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // ─────────────────────────────────────────────────────────────────────
    // Implementación de UserDetails
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Devuelve las autoridades (roles) del usuario en formato Spring Security.
     * Cada rol se prefija con {@code ROLE_} según la convención de Spring Security
     * (ej. {@code ROLE_USER}, {@code ROLE_ADMIN}).
     *
     * @return colección de {@link GrantedAuthority} derivada de los roles del usuario
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el identificador de inicio de sesión del usuario.
     * En Qvenly, el correo electrónico actúa como nombre de usuario.
     *
     * @return correo electrónico del usuario
     */
    @Override
    public String getUsername() { return this.email; }

    /**
     * Devuelve la contraseña del usuario con hash BCrypt.
     * Puede ser {@code null} para usuarios de Google OAuth2.
     *
     * @return contraseña hasheada o {@code null} si el usuario usa OAuth2
     */
    @Override
    public String getPassword() { return this.password; }

    /**
     * Indica si la cuenta del usuario no ha expirado.
     * Qvenly no implementa expiración de cuentas por tiempo.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    /**
     * Indica si la cuenta del usuario no está bloqueada.
     * Qvenly no implementa bloqueo de cuentas actualmente.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isAccountNonLocked() { return true; }

    /**
     * Indica si las credenciales del usuario no han expirado.
     * Qvenly no implementa expiración de credenciales.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Indica si la cuenta del usuario está habilitada para iniciar sesión.
     * Refleja el valor del campo {@link #isActive}.
     *
     * @return {@code true} si la cuenta está activa; {@code false} en caso contrario
     */
    @Override
    public boolean isEnabled() { return this.isActive; }

    // ─────────────────────────────────────────────────────────────────────
    // Callbacks de ciclo de vida JPA
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Inicializa las fechas de auditoría antes de persistir el usuario por primera vez.
     * Establece {@link #createdAt} y {@link #updatedAt} con la fecha y hora actuales.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza la fecha de última modificación antes de cada actualización en base de datos.
     * Se ejecuta automáticamente por JPA cuando se llama a {@code save()} en el repositorio.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}