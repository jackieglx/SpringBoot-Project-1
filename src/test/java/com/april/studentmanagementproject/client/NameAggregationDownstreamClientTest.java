package com.april.studentmanagementproject.client;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.service.RecoveryService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameAggregationDownstreamClientTest {

    private static final String DOWNSTREAM_URL = "http://downstream.example/name/aggregation";

    @Test
    void retriesFailuresAndDoesNotPersistWhenThirdAttemptSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        TestRecoveryService recoveryService = new TestRecoveryService();
        NameAggregationDownstreamClient client = newClient(request -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 3) {
                return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            return Mono.just(jsonResponse(HttpStatus.OK, "{\"name\":[\"Jessica\",\"April\",\"Allen\"]}"));
        }, recoveryService);

        StepVerifier.create(client.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April", "Allen")))
                .verifyComplete();

        assertEquals(3, calls.get());
        assertEquals(0, recoveryService.recoveryCount.get());
    }

    @Test
    void persistsOnceAndReturnsDowngradeResponseWhenAllRetriesFail() {
        AtomicInteger calls = new AtomicInteger();
        TestRecoveryService recoveryService = new TestRecoveryService();
        NameAggregationDownstreamClient client = newClient(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }, recoveryService);

        StepVerifier.create(client.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April")))
                .verifyComplete();

        assertEquals(3, calls.get());
        assertEquals(1, recoveryService.recoveryCount.get());
    }

    @Test
    void openCircuitDoesNotCallDownstreamAndRunsRecovery() {
        AtomicInteger calls = new AtomicInteger();
        TestRecoveryService recoveryService = new TestRecoveryService();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testOpen", circuitBreakerConfig());
        circuitBreaker.transitionToOpenState();

        NameAggregationDownstreamClient client = newClient(
                request -> {
                    calls.incrementAndGet();
                    return Mono.just(jsonResponse(HttpStatus.OK, "{\"name\":[\"Jessica\",\"April\",\"Allen\"]}"));
                },
                circuitBreaker,
                retry(),
                recoveryService);

        StepVerifier.create(client.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April")))
                .verifyComplete();

        assertEquals(0, calls.get());
        assertEquals(1, recoveryService.recoveryCount.get());
    }

    @Test
    void clientErrorDoesNotRetry() {
        AtomicInteger calls = new AtomicInteger();
        TestRecoveryService recoveryService = new TestRecoveryService();
        NameAggregationDownstreamClient client = newClient(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
        }, recoveryService);

        StepVerifier.create(client.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April")))
                .verifyComplete();

        assertEquals(1, calls.get());
        assertEquals(1, recoveryService.recoveryCount.get());
    }

    @Test
    void serverErrorAndTimeoutRetryAccordingToConfiguration() {
        AtomicInteger serverErrorCalls = new AtomicInteger();
        TestRecoveryService serverErrorRecovery = new TestRecoveryService();
        NameAggregationDownstreamClient serverErrorClient = newClient(request -> {
            serverErrorCalls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }, serverErrorRecovery);

        StepVerifier.create(serverErrorClient.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April")))
                .verifyComplete();

        AtomicInteger timeoutCalls = new AtomicInteger();
        TestRecoveryService timeoutRecovery = new TestRecoveryService();
        NameAggregationDownstreamClient timeoutClient = newClient(request -> {
            timeoutCalls.incrementAndGet();
            return Mono.error(new TimeoutException("timeout"));
        }, timeoutRecovery);

        StepVerifier.create(timeoutClient.send(request(), DOWNSTREAM_URL))
                .expectNextMatches(response -> response.getName().equals(List.of("Jessica", "April")))
                .verifyComplete();

        assertEquals(3, serverErrorCalls.get());
        assertEquals(1, serverErrorRecovery.recoveryCount.get());
        assertEquals(3, timeoutCalls.get());
        assertEquals(1, timeoutRecovery.recoveryCount.get());
    }

    private NameAggregationDownstreamClient newClient(
            ExchangeFunction exchangeFunction,
            TestRecoveryService recoveryService) {
        return newClient(exchangeFunction, CircuitBreaker.of("test", circuitBreakerConfig()), retry(), recoveryService);
    }

    private NameAggregationDownstreamClient newClient(
            ExchangeFunction exchangeFunction,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TestRecoveryService recoveryService) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        return new NameAggregationDownstreamClient(builder, circuitBreaker, retry, recoveryService);
    }

    private CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();
    }

    private Retry retry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .retryOnException(ex -> {
                    if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                        return false;
                    }
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        return true;
                    }
                    if (ex instanceof com.april.studentmanagementproject.exception.DownstreamNameAggregationException downstream) {
                        return downstream.getStatusCode() != null && downstream.getStatusCode().is5xxServerError();
                    }
                    return false;
                })
                .build();
        return Retry.of("test", config);
    }

    private NameAggregationRequest request() {
        return new NameAggregationRequest(List.of("Jessica", "April"));
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static class TestRecoveryService implements RecoveryService {

        private final AtomicInteger recoveryCount = new AtomicInteger();

        @Override
        public Mono<NameAggregationResponse> recover(
                NameAggregationRequest request,
                String downstreamUrl,
                Throwable error) {
            recoveryCount.incrementAndGet();
            return Mono.just(new NameAggregationResponse(request.getName()));
        }
    }
}
