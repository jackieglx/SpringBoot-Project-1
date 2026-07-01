package com.april.studentmanagementproject.repository;

import com.april.studentmanagementproject.entity.FailedNameAggregationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedNameAggregationRequestRepository extends JpaRepository<FailedNameAggregationRequest, Long> {
}
