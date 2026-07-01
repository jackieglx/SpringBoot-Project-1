package com.april.studentmanagementproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NameAggregationRequest {

    @NotEmpty(message = "Name list is required")
    private List<@NotBlank(message = "Name cannot be blank") String> name;
}
