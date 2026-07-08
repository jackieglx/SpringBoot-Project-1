package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KafkaMessageConsumer {

    private final KafkaMessageService kafkaMessageService;

    public KafkaMessageConsumer(KafkaMessageService kafkaMessageService) {
        this.kafkaMessageService = kafkaMessageService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic-name}",
            groupId = "${app.kafka.consumer-group-id}",
            concurrency = "${app.kafka.listener-concurrency}")
    public void consume(ConsumerRecord<String, String> record) {
        kafkaMessageService.recordConsumed(new ConsumedKafkaMessage(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value(),
                Thread.currentThread().getName(),
                Instant.now()));
    }
}
