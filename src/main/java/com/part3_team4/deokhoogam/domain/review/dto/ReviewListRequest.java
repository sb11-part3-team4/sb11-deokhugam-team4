package com.part3_team4.deokhoogam.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ReviewListRequest (
    UUID userId,
    UUID bookId,
    String keyword,

    @Pattern(regexp = "createdAt|rating", message = "orderBy는 createdAt 또는 rating만 허용됩니다")
    String orderBy,

    @Pattern(regexp = "ASC|DESC", message = "direction은 ASC 또는 DESC만 허용됩니다")
    String direction,

    String cursor,
    String after,

    @Min(value = 1, message = "limit은 1 이상이어야 합니다")
    @Max(value = 100, message = "limit은 100 이하여야 합니다")
    int limit
) {}
