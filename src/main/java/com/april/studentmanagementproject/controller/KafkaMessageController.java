package com.april.studentmanagementproject.controller;

import com.april.studentmanagementproject.dto.ConsumedKafkaMessage;
import com.april.studentmanagementproject.dto.KafkaMessageRequest;
import com.april.studentmanagementproject.dto.KafkaMessageResponse;
import com.april.studentmanagementproject.service.KafkaMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/kafka/messages")
public class KafkaMessageController {

    private final KafkaMessageService kafkaMessageService;

    public KafkaMessageController(KafkaMessageService kafkaMessageService) {
        this.kafkaMessageService = kafkaMessageService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<KafkaMessageResponse>> publishMessage(
            @Valid @RequestBody KafkaMessageRequest request) {
        return kafkaMessageService.publish(request.getKey(), request.getValue())
                .thenApply(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }

    @GetMapping("/consumed")
    public ResponseEntity<List<ConsumedKafkaMessage>> getConsumedMessages() {
        return ResponseEntity.ok(kafkaMessageService.getConsumedMessages());
    }
}
