package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import reactor.core.publisher.Mono;

public interface NameAggregationService {

    Mono<NameAggregationResponse> aggregateNames(NameAggregationRequest request, String requestDownstreamUrl);
}
