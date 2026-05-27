package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class ReviewNotFoundException extends ReviewException {

  private ReviewNotFoundException() {
    super(ErrorCode.REVIEW_NOT_FOUND);
  }

  public static ReviewNotFoundException withId(UUID reviewId) {
    ReviewNotFoundException exception = new ReviewNotFoundException();
    exception.addDetail(ErrorKey.REVIEW_ID, reviewId);
    return exception;
  }
}