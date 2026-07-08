package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.config.StudentKafkaProperties;
import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import com.april.studentmanagementproject.dto.KafkaMessageResponse;
import com.april.studentmanagementproject.service.KafkaMessageService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class KafkaMessageServiceImpl implements KafkaMessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StudentKafkaProperties kafkaProperties;
    private final ConcurrentLinkedDeque<ConsumedKafkaMessage> consumedMessages = new ConcurrentLinkedDeque<>();

    public KafkaMessageServiceImpl(
            KafkaTemplate<String, String> kafkaTemplate,
            StudentKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public CompletableFuture<KafkaMessageResponse> publish(String key, String value) {
        return kafkaTemplate.send(kafkaProperties.getTopicName(), key, value)
                .thenApply(result -> {
                    RecordMetadata metadata = result.getRecordMetadata();
                    return new KafkaMessageResponse(
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            key,
                            value);
                });
    }

    @Override
    public void recordConsumed(ConsumedKafkaMessage message) {
        consumedMessages.addFirst(message);
        while (consumedMessages.size() > kafkaProperties.getConsumedMessageHistoryLimit()) {
            consumedMessages.pollLast();
        }
    }

    @Override
    public List<ConsumedKafkaMessage> getConsumedMessages() {
        return new ArrayList<>(consumedMessages);
    }
}
