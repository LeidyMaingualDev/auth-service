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
 * @author Leidy Martinez
 * @version 4.0
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = true)
    private String password;

    /**
     * Número de documento. Obligatorio para usuarios LOCAL.
     * Para usuarios GOOGLE se completa en el formulario de perfil.
     */
    @Column(name = "document_number", unique = true, nullable = false)
    private String documentNumber;

    /**
     * Tipo de documento. Obligatorio para usuarios LOCAL.
     * Para usuarios GOOGLE se completa en el formulario de perfil.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    /**
     * Teléfono. Obligatorio para usuarios LOCAL.
     * Para usuarios GOOGLE se completa en el formulario de perfil.
     */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // ─── UserDetails ──────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override public String getUsername()              { return this.email; }
    @Override public String getPassword()              { return this.password; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return this.isActive; }

    /**
     * Indica si el perfil del usuario está completo.
     * Un usuario de Google tiene el perfil incompleto si le faltan
     * documentNumber, documentType o phoneNumber.
     *
     * @return true si todos los datos obligatorios están presentes
     */
    public boolean isProfileComplete() {
        return documentNumber != null
                && !documentNumber.isBlank()
                && !documentNumber.equals("PENDIENTE")
                && documentType != null
                && phoneNumber != null
                && !phoneNumber.isBlank()
                && !phoneNumber.equals("PENDIENTE");
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}