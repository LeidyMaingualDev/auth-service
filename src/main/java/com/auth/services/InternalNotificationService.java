package com.auth.services;

import com.auth.models.dtos.InternalNotificationRequestDTO;
import com.auth.models.entities.NotificationInbox;
import com.auth.models.enums.NotificationStatus;
import com.auth.repositories.NotificationInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio interno para guardar notificaciones enviadas por otros microservicios.
 * Solo qv-ms-notifications debe llamar a este servicio vía red interna.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalNotificationService {

    private final NotificationInboxRepository inboxRepository;

    @Transactional
    public void save(InternalNotificationRequestDTO req) {
        NotificationInbox notification = NotificationInbox.builder()
                .userId(req.getUserId())
                .recipientEmail(req.getRecipientEmail())
                .recipientName(req.getRecipientName())
                .type(req.getType())
                .title(req.getTitle())
                .message(req.getMessage())
                .read(false)
                .silent(false)
                .status(NotificationStatus.STORED)
                .retryCount(0)
                .build();

        inboxRepository.save(notification);
        log.info("Notificación interna guardada para userId={}, tipo={}",
                req.getUserId(), req.getType());
    }
}