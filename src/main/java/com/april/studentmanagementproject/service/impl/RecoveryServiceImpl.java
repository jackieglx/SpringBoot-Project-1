package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.entity.FailedNameAggregationRequest;
import com.april.studentmanagementproject.repository.FailedNameAggregationRequestRepository;
import com.april.studentmanagementproject.service.RecoveryService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RecoveryServiceImpl implements RecoveryService {

    private final FailedNameAggregationRequestRepository failedRequestRepository;

    public RecoveryServiceImpl(FailedNameAggregationRequestRepository failedRequestRepository) {
        this.failedRequestRepository = failedRequestRepository;
    }

    @Override
    public Mono<NameAggregationResponse> recover(NameAggregationRequest request, String downstreamUrl, Throwable error) {
        return Mono.fromCallable(() -> failedRequestRepository.save(new FailedNameAggregationRequest(
                        serializeRequest(request),
                        downstreamUrl,
                        error.getMessage())))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(new NameAggregationResponse(request.getName()));
    }

    private String serializeRequest(NameAggregationRequest request) {
        String names = request.getName().stream()
                .map(this::quoteJsonString)
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        return "{\"name\":[" + names + "]}";
    }

    private String quoteJsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
