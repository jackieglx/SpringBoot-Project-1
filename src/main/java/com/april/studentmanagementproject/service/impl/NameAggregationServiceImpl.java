package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.client.NameAggregationDownstreamClient;
import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.exception.NameAggregationConfigurationException;
import com.april.studentmanagementproject.service.NameAggregationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class NameAggregationServiceImpl implements NameAggregationService {

    private final NameAggregationDownstreamClient downstreamClient;
    private final String serviceName;
    private final String downstreamUrl;

    public NameAggregationServiceImpl(
            NameAggregationDownstreamClient downstreamClient,
            @Value("${name.aggregation.service-name:April}") String serviceName,
            @Value("${downstream.name-aggregation.url:}") String downstreamUrl) {
        this.downstreamClient = downstreamClient;
        this.serviceName = serviceName;
        this.downstreamUrl = downstreamUrl;
    }

    @Override
    public Mono<NameAggregationResponse> aggregateNames(NameAggregationRequest request, String requestDownstreamUrl) {
        String localName = requireConfiguredServiceName();
        String targetUrl = resolveDownstreamUrl(requestDownstreamUrl);

        List<String> updatedNames = new ArrayList<>(request.getName());
        updatedNames.add(localName);

        NameAggregationRequest downstreamRequest = new NameAggregationRequest(updatedNames);

        if (targetUrl == null) {
            return Mono.just(new NameAggregationResponse(updatedNames));
        }

        return downstreamClient.send(downstreamRequest, targetUrl);
    }

    private String requireConfiguredServiceName() {
        if (serviceName == null || serviceName.isBlank()) {
            throw new NameAggregationConfigurationException("Name aggregation service name is not configured");
        }
        return serviceName;
    }

    private String resolveDownstreamUrl(String requestDownstreamUrl) {
        if (requestDownstreamUrl != null && !requestDownstreamUrl.isBlank()) {
            return requestDownstreamUrl;
        }

        if (downstreamUrl != null && !downstreamUrl.isBlank()) {
            return downstreamUrl;
        }

        return null;
    }
}
