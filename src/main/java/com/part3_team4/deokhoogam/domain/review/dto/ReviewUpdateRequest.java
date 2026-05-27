package com.part3_team4.deokhoogam.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewUpdateRequest (
  @NotNull @Min(1) @Max(5) int rating,
  @NotBlank String content
) {}
