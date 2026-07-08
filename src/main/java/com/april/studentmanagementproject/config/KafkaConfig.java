package com.april.studentmanagementproject.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(StudentKafkaProperties.class)
public class KafkaConfig {

    @Bean
    public NewTopic studentEventsTopic(StudentKafkaProperties kafkaProperties) {
        return TopicBuilder.name(kafkaProperties.getTopicName())
                .partitions(kafkaProperties.getPartitions())
                .replicas(kafkaProperties.getReplicationFactor())
                .build();
    }
}
