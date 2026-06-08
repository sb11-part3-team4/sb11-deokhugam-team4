package com.part3_team4.deokhoogam.domain.review.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PopularReviewResponse(
        UUID id,
        UUID reviewId,
        UUID bookId,
        String bookTitle,
        String bookThumbnailUrl,
        UUID userId,
        String userNickname,
        String reviewContent,
        int reviewRating,
        String period,
        Instant createdAt,
        int rank,
        BigDecimal score,
        int likeCount,
        int commentCount
) {}