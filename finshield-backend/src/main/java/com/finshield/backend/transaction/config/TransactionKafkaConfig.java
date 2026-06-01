package com.finshield.backend.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TransactionKafkaConfig {

    public static final String TRANSACTION_INCOMING_TOPIC = "transaction.incoming";
    public static final String TRANSACTION_RISK_SCORED_TOPIC = "transaction.risk.scored";
    public static final String TRANSACTION_INCOMING_DLT = "transaction.incoming.DLT";

    @Bean
    NewTopic transactionIncomingTopic(
            @Value("${finshield.kafka.transaction-incoming.partitions:6}") int partitions,
            @Value("${finshield.kafka.transaction-incoming.replicas:1}") int replicas
    ) {
        return TopicBuilder.name(TRANSACTION_INCOMING_TOPIC)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    NewTopic transactionRiskScoredTopic(
            @Value("${finshield.kafka.transaction-risk-scored.partitions:6}") int partitions,
            @Value("${finshield.kafka.transaction-risk-scored.replicas:1}") int replicas
    ) {
        return TopicBuilder.name(TRANSACTION_RISK_SCORED_TOPIC)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    NewTopic transactionIncomingDeadLetterTopic(
            @Value("${finshield.kafka.transaction-incoming.partitions:6}") int partitions,
            @Value("${finshield.kafka.transaction-incoming.replicas:1}") int replicas
    ) {
        return TopicBuilder.name(TRANSACTION_INCOMING_DLT)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
