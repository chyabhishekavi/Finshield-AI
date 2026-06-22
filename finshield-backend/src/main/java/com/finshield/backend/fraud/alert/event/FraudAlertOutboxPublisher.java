package com.finshield.backend.fraud.alert.event;

import com.finshield.backend.fraud.alert.domain.FraudAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.finshield.backend.fraud.alert.config.FraudAlertKafkaConfig.FRAUD_ALERT_CREATED_TOPIC;

@Component
public class FraudAlertOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(FraudAlertOutboxPublisher.class);
    private final FraudAlertEventOutboxRepository repository;
    private final KafkaTemplate<String, FraudAlertCreatedEvent> kafkaTemplate;

    public FraudAlertOutboxPublisher(FraudAlertEventOutboxRepository repository,
            KafkaTemplate<String, FraudAlertCreatedEvent> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${finshield.kafka.fraud-alert-created.publish-delay:2s}")
    @Transactional
    public void publishPending() {
        for (FraudAlertEventOutbox outbox : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                FraudAlert alert = outbox.getAlert();
                FraudAlertCreatedEvent event = new FraudAlertCreatedEvent(outbox.getEventId(), alert.getId(),
                        outbox.getAlertNumber(), outbox.getTransactionId(), outbox.getTransactionReference(),
                        outbox.getCustomerId(), outbox.getRiskScore(), outbox.getRiskBand(),
                        outbox.getSeverity(), outbox.getStatus(), outbox.getOccurredAt());
                kafkaTemplate.send(FRAUD_ALERT_CREATED_TOPIC, outbox.getAlertNumber(), event)
                        .get(10, TimeUnit.SECONDS);
                outbox.published(Instant.now());
            } catch (Exception exception) {
                outbox.failed(exception.getMessage());
                log.warn("Fraud alert event publish failed; outboxId={}, attempt={}",
                        outbox.getId(), outbox.getAttempts(), exception);
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
