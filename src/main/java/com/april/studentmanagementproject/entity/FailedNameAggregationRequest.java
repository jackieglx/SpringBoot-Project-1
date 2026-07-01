package com.april.studentmanagementproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "failed_name_aggregation_requests")
public class FailedNameAggregationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_body", columnDefinition = "TEXT", nullable = false)
    private String requestBody;

    @Column(name = "downstream_url", nullable = false)
    private String downstreamUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    public FailedNameAggregationRequest(String requestBody, String downstreamUrl, String errorMessage) {
        this.requestBody = requestBody;
        this.downstreamUrl = downstreamUrl;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastAttemptedAt = now;
    }
}
