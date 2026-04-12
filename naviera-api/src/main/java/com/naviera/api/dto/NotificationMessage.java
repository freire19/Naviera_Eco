package com.naviera.api.dto;

import java.time.Instant;

public record NotificationMessage(
    String type,
    String entity,
    Object entityId,
    String message,
    Instant timestamp
) {
    public NotificationMessage(String type, String entity, Object entityId, String message) {
        this(type, entity, entityId, message, Instant.now());
    }
}
