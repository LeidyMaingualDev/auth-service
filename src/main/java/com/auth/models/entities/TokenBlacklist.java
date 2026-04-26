package com.auth.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "invalidated_at", nullable = false)
    private LocalDateTime invalidatedAt;

    @PrePersist
    protected void onCreate() {
        this.invalidatedAt = LocalDateTime.now();
    }
}