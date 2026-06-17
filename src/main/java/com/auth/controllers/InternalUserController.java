package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.entities.User;
import com.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint interno para que otros microservicios consulten si un email
 * corresponde a un usuario registrado. NO se expone por el Gateway —
 * solo red interna.
 */
@RestController
@RequestMapping("/auth/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/by-email")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> findByEmail(
            @RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(ApiResponseDTO.ok("Usuario encontrado",
                        Map.<String, Object>of(
                                "userId", u.getId(),
                                "name", u.getName(),
                                "lastName", u.getLastName() != null ? u.getLastName() : ""
                        ))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.ok("Usuario no encontrado", null)));
    }
}