package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

import java.util.UUID;

public class ReviewLikeAlreadyExistsException extends ReviewException {
    private ReviewLikeAlreadyExistsException() {
        super(ErrorCode.REVIEW_LIKE_ALREADY_EXISTS);
    }

    public static ReviewLikeAlreadyExistsException withReviewIdAndUserId(UUID reviewId, UUID userId) {
        ReviewLikeAlreadyExistsException exception = new ReviewLikeAlreadyExistsException();
        exception.addDetail(ErrorKey.REVIEW_ID, reviewId);
        exception.addDetail(ErrorKey.USER_ID, userId);
        return exception;
    }
}