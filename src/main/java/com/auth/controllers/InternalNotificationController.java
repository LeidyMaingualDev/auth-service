package com.auth.controllers;

import com.auth.models.dtos.ApiResponseDTO;
import com.auth.models.dtos.InternalNotificationRequestDTO;
import com.auth.services.InternalNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno para que qv-ms-notifications inserte notificaciones
 * en la bandeja del usuario. NO se expone por el Gateway — solo red interna.
 */
@RestController
@RequestMapping("/auth/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final InternalNotificationService service;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<Void>> save(
            @Valid @RequestBody InternalNotificationRequestDTO request) {
        service.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Notificación guardada", null));
    }
}