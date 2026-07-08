package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.config.StudentKafkaProperties;
import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaMessageServiceImplTest {

    @SuppressWarnings("unchecked")
    @Test
    void publishesMessageToConfiguredTopicAndReturnsKafkaMetadata() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        StudentKafkaProperties properties = new StudentKafkaProperties();
        properties.setTopicName("student-events");
        KafkaMessageServiceImpl service = new KafkaMessageServiceImpl(kafkaTemplate, properties);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("student-events", "student-1", "created");
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("student-events", 1),
                42L,
                0,
                0L,
                9,
                7);
        SendResult<String, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
        when(kafkaTemplate.send("student-events", "student-1", "created"))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        var response = service.publish("student-1", "created").join();

        assertEquals("student-events", response.getTopic());
        assertEquals(1, response.getPartition());
        assertEquals(42L, response.getOffset());
        assertEquals("student-1", response.getKey());
        assertEquals("created", response.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    void keepsOnlyConfiguredNumberOfConsumedMessages() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        StudentKafkaProperties properties = new StudentKafkaProperties();
        properties.setConsumedMessageHistoryLimit(2);
        KafkaMessageServiceImpl service = new KafkaMessageServiceImpl(kafkaTemplate, properties);

        service.recordConsumed(message(0L, "first"));
        service.recordConsumed(message(1L, "second"));
        service.recordConsumed(message(2L, "third"));

        var messages = service.getConsumedMessages();

        assertEquals(2, messages.size());
        assertEquals("third", messages.get(0).getValue());
        assertEquals("second", messages.get(1).getValue());
    }

    private ConsumedKafkaMessage message(long offset, String value) {
        return new ConsumedKafkaMessage(
                "student-events",
                0,
                offset,
                "student-1",
                value,
                "test-thread",
                Instant.now());
    }
}
