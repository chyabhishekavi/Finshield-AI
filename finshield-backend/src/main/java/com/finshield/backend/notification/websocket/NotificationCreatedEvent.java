package com.finshield.backend.notification.websocket;

import com.finshield.backend.notification.api.NotificationResponse;

public record NotificationCreatedEvent(String recipientUsername, NotificationResponse notification) {}
