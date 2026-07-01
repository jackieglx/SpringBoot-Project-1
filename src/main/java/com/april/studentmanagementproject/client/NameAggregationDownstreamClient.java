package com.april.studentmanagementproject.client;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.exception.DownstreamNameAggregationException;
import com.april.studentmanagementproject.service.RecoveryService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class NameAggregationDownstreamClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RecoveryService recoveryService;

    public NameAggregationDownstreamClient(
            WebClient.Builder webClientBuilder,
            CircuitBreaker nameAggregationCircuitBreaker,
            Retry nameAggregationRetry,
            RecoveryService recoveryService) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = nameAggregationCircuitBreaker;
        this.retry = nameAggregationRetry;
        this.recoveryService = recoveryService;
    }

    public Mono<NameAggregationResponse> send(NameAggregationRequest request, String downstreamUrl) {
        return callDownstream(request, downstreamUrl)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .onErrorResume(error -> recoveryService.recover(request, downstreamUrl, error));
    }

    private Mono<NameAggregationResponse> callDownstream(NameAggregationRequest request, String downstreamUrl) {
        return webClient.post()
                .uri(downstreamUrl)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new DownstreamNameAggregationException(
                        "Downstream name aggregation service returned client status " + response.statusCode(),
                        response.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new DownstreamNameAggregationException(
                        "Downstream name aggregation service returned server status " + response.statusCode(),
                        response.statusCode())))
                .bodyToMono(NameAggregationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(ex -> !(ex instanceof DownstreamNameAggregationException),
                        ex -> new DownstreamNameAggregationException(
                                "Failed to call downstream name aggregation service",
                                ex,
                                HttpStatus.BAD_GATEWAY))
                .flatMap(this::validateDownstreamResponse);
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
}
