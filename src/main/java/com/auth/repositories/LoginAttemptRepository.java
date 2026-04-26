package com.auth.repositories;

import com.auth.models.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    // Contar intentos fallidos recientes por email
    long countByEmailAndSuccessAndAttemptedAtAfter(
            String email, boolean success, LocalDateTime after
    );

    // Obtener los últimos intentos fallidos
    List<LoginAttempt> findByEmailAndSuccessOrderByAttemptedAtDesc(
            String email, boolean success
    );
}