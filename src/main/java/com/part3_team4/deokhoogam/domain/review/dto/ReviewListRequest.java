package com.part3_team4.deokhoogam.domain.review.dto;

import java.util.UUID;

public record ReviewListRequest (
    UUID userId,
    UUID bookId,
    String keyword,
    String orderBy,
    String direction,
    String cursor,
    String after,
    int limit
) {}
