package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.entity.FailedNameAggregationRequest;
import com.april.studentmanagementproject.exception.DownstreamNameAggregationException;
import com.april.studentmanagementproject.exception.NameAggregationConfigurationException;
import com.april.studentmanagementproject.repository.FailedNameAggregationRequestRepository;
import com.april.studentmanagementproject.service.NameAggregationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class NameAggregationServiceImpl implements NameAggregationService {

    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final WebClient webClient;
    private final CircuitBreaker downstreamCircuitBreaker;
    private final FailedNameAggregationRequestRepository failedRequestRepository;
    private final String serviceName;
    private final String downstreamUrl;

    public NameAggregationServiceImpl(
            WebClient.Builder webClientBuilder,
            FailedNameAggregationRequestRepository failedRequestRepository,
            @Value("${name.aggregation.service-name:April}") String serviceName,
            @Value("${downstream.name-aggregation.url:}") String downstreamUrl) {
        this.webClient = webClientBuilder.build();
        this.downstreamCircuitBreaker = CircuitBreaker.of(
                "nameAggregationDownstream",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .minimumNumberOfCalls(2)
                        .slidingWindowSize(5)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build());
        this.failedRequestRepository = failedRequestRepository;
        this.serviceName = serviceName;
        this.downstreamUrl = downstreamUrl;
    }

    @Override
    public Mono<NameAggregationResponse> aggregateNames(NameAggregationRequest request, String requestDownstreamUrl) {
        String localName = requireConfiguredServiceName();
        String targetUrl = resolveDownstreamUrl(requestDownstreamUrl);

        List<String> updatedNames = new ArrayList<>(request.getName());
        updatedNames.add(localName);

        if (targetUrl == null) {
            return Mono.just(new NameAggregationResponse(updatedNames));
        }

        NameAggregationRequest downstreamRequest = new NameAggregationRequest(updatedNames);
        return callDownstream(targetUrl, downstreamRequest)
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofMillis(300))
                        .filter(this::isRetryableDownstreamFailure))
                .transformDeferred(CircuitBreakerOperator.of(downstreamCircuitBreaker))
                .onErrorResume(DownstreamNameAggregationException.class,
                        ex -> persistFailedRequest(downstreamRequest, targetUrl, ex)
                                .thenReturn(new NameAggregationResponse(updatedNames)));
    }

    private Mono<NameAggregationResponse> callDownstream(String targetUrl, NameAggregationRequest downstreamRequest) {
        return webClient.post()
                .uri(targetUrl)
                .bodyValue(downstreamRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new DownstreamNameAggregationException(
                        "Downstream name aggregation service returned status " + response.statusCode(),
                        HttpStatus.BAD_GATEWAY)))
                .bodyToMono(NameAggregationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(this::isWebClientFailure, ex -> new DownstreamNameAggregationException(
                        "Failed to call downstream name aggregation service",
                        ex,
                        HttpStatus.BAD_GATEWAY))
                .flatMap(this::validateDownstreamResponse);
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

    private Mono<NameAggregationResponse> validateDownstreamResponse(NameAggregationResponse response) {
        if (response == null || response.getName() == null || response.getName().isEmpty()) {
            return Mono.error(new DownstreamNameAggregationException(
                    "Downstream name aggregation response is empty",
                    HttpStatus.BAD_GATEWAY));
        }

        boolean hasBlankName = response.getName().stream()
                .anyMatch(name -> name == null || name.isBlank());

        if (hasBlankName) {
            return Mono.error(new DownstreamNameAggregationException(
                    "Downstream name aggregation response contains a blank name",
                    HttpStatus.BAD_GATEWAY));
        }

        return Mono.just(response);
    }

    private boolean isRetryableDownstreamFailure(Throwable ex) {
        return ex instanceof DownstreamNameAggregationException;
    }

    private boolean isWebClientFailure(Throwable ex) {
        return ex instanceof WebClientRequestException
                || ex instanceof WebClientResponseException
                || ex instanceof TimeoutException;
    }

    private Mono<Void> persistFailedRequest(
            NameAggregationRequest downstreamRequest,
            String targetUrl,
            DownstreamNameAggregationException ex) {
        return Mono.fromRunnable(() -> failedRequestRepository.save(new FailedNameAggregationRequest(
                serializeRequest(downstreamRequest),
                targetUrl,
                ex.getMessage())))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private String serializeRequest(NameAggregationRequest downstreamRequest) {
        String names = downstreamRequest.getName().stream()
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
