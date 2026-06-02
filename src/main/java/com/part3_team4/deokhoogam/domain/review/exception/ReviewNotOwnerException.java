package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class ReviewNotOwnerException extends ReviewException {

    private ReviewNotOwnerException() {
        super(ErrorCode.REVIEW_NOT_OWNER);
    }

    public static ReviewNotOwnerException withUserId(UUID userId) {
        ReviewNotOwnerException exception = new ReviewNotOwnerException();
        exception.addDetail(ErrorKey.USER_ID, userId);
        return exception;
    }
}
