package com.auth.models.dtos;

import com.auth.models.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InternalNotificationRequestDTO {

    @NotNull(message = "El userId es obligatorio")
    private Long userId;

    @NotBlank(message = "El email del destinatario es obligatorio")
    private String recipientEmail;

    private String recipientName;

    @NotNull(message = "El tipo de notificación es obligatorio")
    private NotificationType type;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;
}