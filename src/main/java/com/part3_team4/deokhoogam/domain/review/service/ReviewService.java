package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, ReviewCreateRequest request);
    ReviewResponse getReview(UUID reviewId, UUID userId);
}
