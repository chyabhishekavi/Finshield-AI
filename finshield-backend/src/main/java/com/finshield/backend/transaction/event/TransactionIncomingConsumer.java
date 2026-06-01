package com.finshield.backend.transaction.event;

import com.finshield.backend.risk.RiskScoringResult;
import com.finshield.backend.risk.RiskScoringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.finshield.backend.transaction.config.TransactionKafkaConfig.TRANSACTION_INCOMING_TOPIC;

@Component
public class TransactionIncomingConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionIncomingConsumer.class);
    private static final int SCORED_EVENT_SCHEMA_VERSION = 1;

    private final RiskScoringService riskScoringService;
    private final TransactionRiskScoredPublisher riskScoredPublisher;

    public TransactionIncomingConsumer(
            RiskScoringService riskScoringService,
            TransactionRiskScoredPublisher riskScoredPublisher
    ) {
        this.riskScoringService = riskScoringService;
        this.riskScoredPublisher = riskScoredPublisher;
    }

    @KafkaListener(topics = TRANSACTION_INCOMING_TOPIC)
    public void consume(ConsumerRecord<String, TransactionEvent> record) {
        TransactionEvent event = record.value();
        if (event == null) {
            throw new IllegalArgumentException("Transaction event must not be null");
        }
        if (!event.transactionReference().equals(record.key())) {
            throw new IllegalArgumentException("Kafka record key does not match transaction reference");
        }

        log.info(
                "Scoring transaction event. eventId={}, reference={}, partition={}, offset={}",
                event.eventId(), event.transactionReference(), record.partition(), record.offset()
        );

        RiskScoringResult result = riskScoringService.score(event);
        riskScoredPublisher.publish(new TransactionRiskScoredEvent(
                deterministicEventId(event.eventId()),
                event.eventId(),
                SCORED_EVENT_SCHEMA_VERSION,
                event.transactionId(),
                event.transactionReference(),
                result.finalScore(),
                result.riskBand(),
                result.decision(),
                result.scoringVersion(),
                result.scoredAt()
        ));

        log.info(
                "Transaction risk scoring completed. eventId={}, reference={}, band={}, decision={}",
                event.eventId(), event.transactionReference(), result.riskBand(), result.decision()
        );
    }

    private UUID deterministicEventId(UUID causationEventId) {
        return UUID.nameUUIDFromBytes(
                ("transaction-risk-scored:" + causationEventId).getBytes(StandardCharsets.UTF_8)
        );
    }
}
