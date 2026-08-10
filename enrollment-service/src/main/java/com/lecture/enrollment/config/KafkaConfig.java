package com.lecture.enrollment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.provision-status-changed}")
    private String provisionStatusChangedTopic;

    @Value("${kafka.topic.resource-provided}")
    private String resourceProvidedTopic;

    @Bean
    public NewTopic provisionStatusChangedTopic() {
        return TopicBuilder.name(provisionStatusChangedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic resourceProvidedTopic() {
        return TopicBuilder.name(resourceProvidedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
