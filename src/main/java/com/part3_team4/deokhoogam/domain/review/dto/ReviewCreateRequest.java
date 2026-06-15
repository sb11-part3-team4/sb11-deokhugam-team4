package com.part3_team4.deokhoogam.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewCreateRequest (
    @NotNull UUID bookId,
    @Min(1) @Max(5) int rating,
    @NotBlank String content
) {}
