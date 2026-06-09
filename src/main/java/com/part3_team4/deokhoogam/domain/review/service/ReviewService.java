package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.global.common.PageResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, ReviewCreateRequest request);
    ReviewResponse getReview(UUID reviewId, UUID userId);
    ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewUpdateRequest request);
    void deleteReview(UUID reviewId, UUID userId);
    ReviewLikeResponse toggleLike(UUID reviewId, UUID userId);
    PageResponse<ReviewResponse> getReviews(UUID userId, ReviewListRequest request);
    PageResponse<PopularReviewResponse> getPopularReviews(String period, String direction, String cursor, String after, int limit);
}
