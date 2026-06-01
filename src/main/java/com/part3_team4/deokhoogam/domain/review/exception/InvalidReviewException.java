package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidReviewException extends ReviewException {

    private InvalidReviewException() {
        super(ErrorCode.REVIEW_INVALID_INPUT);
    }

    public static InvalidReviewException withField(ErrorKey field, String reason) {
        InvalidReviewException exception = new InvalidReviewException();
        exception.addDetail(ErrorKey.FIELD, field.getValue());
        exception.addDetail(ErrorKey.REASON, reason);
        return exception;
    }

    public static InvalidReviewException withFieldAndValue(ErrorKey field, Object value, String reason) {
        InvalidReviewException exception = new InvalidReviewException();
        exception.addDetail(ErrorKey.FIELD, field.getValue());
        exception.addDetail(ErrorKey.VALUE, value);
        exception.addDetail(ErrorKey.REASON, reason);
        return exception;
    }
}
