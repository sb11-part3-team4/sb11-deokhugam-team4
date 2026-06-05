package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewLikeResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewUpdateRequest;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, ReviewCreateRequest request);
    ReviewResponse getReview(UUID reviewId, UUID userId);
    ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewUpdateRequest request);
    void deleteReview(UUID reviewId, UUID userId);
    ReviewLikeResponse toggleLike(UUID reviewId, UUID userId);
}
