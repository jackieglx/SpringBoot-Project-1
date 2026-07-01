package com.april.studentmanagementproject.config;

import com.april.studentmanagementproject.exception.DownstreamNameAggregationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.concurrent.TimeoutException;

@Configuration
@EnableConfigurationProperties(NameAggregationResilienceProperties.class)
public class Resilience4jConfig {

    @Bean
    public CircuitBreaker nameAggregationCircuitBreaker(NameAggregationResilienceProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getFailureRateThreshold())
                .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
                .slidingWindowSize(properties.getSlidingWindowSize())
                .waitDurationInOpenState(properties.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState())
                .build();

        return CircuitBreaker.of("nameAggregationDownstream", config);
    }

    @Bean
    public Retry nameAggregationRetry(NameAggregationResilienceProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getMaxRetryAttempts())
                .waitDuration(properties.getRetryWaitDuration())
                .retryOnException(this::isRetryableFailure)
                .build();

        return Retry.of("nameAggregationDownstream", config);
    }

    private boolean isRetryableFailure(Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            return false;
        }

        if (ex instanceof WebClientRequestException || ex instanceof TimeoutException) {
            return true;
        }

        if (ex instanceof DownstreamNameAggregationException downstreamException) {
            HttpStatusCode statusCode = downstreamException.getStatusCode();
            return statusCode != null && statusCode.is5xxServerError();
        }

        return false;
    }
}
