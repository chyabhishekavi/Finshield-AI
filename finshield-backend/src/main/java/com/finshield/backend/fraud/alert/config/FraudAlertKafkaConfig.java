package com.finshield.backend.fraud.alert.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class FraudAlertKafkaConfig {
    public static final String FRAUD_ALERT_CREATED_TOPIC = "fraud.alert.created";

    @Bean
    NewTopic fraudAlertCreatedTopic(
            @Value("${finshield.kafka.fraud-alert-created.partitions:3}") int partitions,
            @Value("${finshield.kafka.fraud-alert-created.replicas:1}") int replicas) {
        return TopicBuilder.name(FRAUD_ALERT_CREATED_TOPIC).partitions(partitions).replicas(replicas).build();
    }
}
