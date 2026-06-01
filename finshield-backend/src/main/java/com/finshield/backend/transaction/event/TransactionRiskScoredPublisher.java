package com.finshield.backend.transaction.event;

import com.finshield.backend.common.exception.ServiceUnavailableException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.finshield.backend.transaction.config.TransactionKafkaConfig.TRANSACTION_RISK_SCORED_TOPIC;

@Component
public class TransactionRiskScoredPublisher {

    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, TransactionRiskScoredEvent> kafkaTemplate;

    public TransactionRiskScoredPublisher(
            KafkaTemplate<String, TransactionRiskScoredEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionRiskScoredEvent event) {
        try {
            kafkaTemplate.send(
                            TRANSACTION_RISK_SCORED_TOPIC,
                            event.transactionReference(),
                            event
                    )
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Risk-scored event publication was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new ServiceUnavailableException("Risk-scored event could not be published", exception);
        }
    }
}
