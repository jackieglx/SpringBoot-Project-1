package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import com.april.studentmanagementproject.dto.KafkaMessageResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface KafkaMessageService {

    CompletableFuture<KafkaMessageResponse> publish(String key, String value);

    void recordConsumed(ConsumedKafkaMessage message);

    List<ConsumedKafkaMessage> getConsumedMessages();
}
