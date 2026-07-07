package com.finshield.backend.notification.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WebSocketNotificationPublisher {
    public static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(event.recipientUsername(),
                USER_NOTIFICATION_DESTINATION, event.notification());
    }
}
