package com.april.studentmanagementproject.exception;

import org.springframework.http.HttpStatusCode;

public class DownstreamNameAggregationException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public DownstreamNameAggregationException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public DownstreamNameAggregationException(String message, Throwable cause, HttpStatusCode statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
