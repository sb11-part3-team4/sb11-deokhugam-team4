package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class ReviewAlreadyExistsException extends ReviewException {

  private ReviewAlreadyExistsException() {
    super(ErrorCode.REVIEW_ALREADY_EXISTS);
  }

  public static ReviewAlreadyExistsException withUserIdAndBookId(UUID userId, UUID bookId) {
    ReviewAlreadyExistsException exception = new ReviewAlreadyExistsException();
    exception.addDetail(ErrorKey.USER_ID, userId);
    exception.addDetail(ErrorKey.BOOK_ID, bookId);
    return exception;
  }
}