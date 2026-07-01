package com.april.studentmanagementproject.controller;

import com.april.studentmanagementproject.dto.NameAggregationRequest;
import com.april.studentmanagementproject.dto.NameAggregationResponse;
import com.april.studentmanagementproject.service.NameAggregationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/name")
public class NameAggregationController {

    private final NameAggregationService nameAggregationService;

    public NameAggregationController(NameAggregationService nameAggregationService) {
        this.nameAggregationService = nameAggregationService;
    }

    @PostMapping("/aggregation")
    public Mono<ResponseEntity<NameAggregationResponse>> aggregateNames(
            @Valid @RequestBody NameAggregationRequest request,
            @RequestHeader(value = "X-Downstream-Url", required = false) String downstreamUrl) {
        return nameAggregationService.aggregateNames(request, downstreamUrl)
                .map(ResponseEntity::ok);
    }
}
