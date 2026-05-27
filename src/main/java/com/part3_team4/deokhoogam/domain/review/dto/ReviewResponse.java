package com.part3_team4.deokhoogam.domain.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse (
        UUID id,
        UUID userId,
        UUID bookId,
        int rating,
        String content,
        int likeCount,
        int commentCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {}
