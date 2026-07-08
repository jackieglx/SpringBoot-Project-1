package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaMessageConsumerTest {

    @Test
    void recordsConsumedKafkaMessageMetadata() {
        KafkaMessageService kafkaMessageService = mock(KafkaMessageService.class);
        KafkaMessageConsumer consumer = new KafkaMessageConsumer(kafkaMessageService);

        consumer.consume(new ConsumerRecord<>("student-events", 2, 17L, "student-1", "created"));

        verify(kafkaMessageService).recordConsumed(argThat((ConsumedKafkaMessage message) ->
                message.getTopic().equals("student-events")
                        && message.getPartition() == 2
                        && message.getOffset() == 17L
                        && message.getKey().equals("student-1")
                        && message.getValue().equals("created")
                        && message.getConsumerThread() != null
                        && message.getConsumedAt() != null));
    }
}
