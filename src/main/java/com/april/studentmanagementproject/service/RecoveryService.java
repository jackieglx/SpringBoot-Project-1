package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import reactor.core.publisher.Mono;

public interface RecoveryService {

    Mono<NameAggregationResponse> recover(NameAggregationRequest request, String downstreamUrl, Throwable error);
}
