package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidBookException extends BookException {

  private InvalidBookException() {
    super(ErrorCode.INVALID_BOOK_INPUT);
  }

  // 필드와 이유를 함께 전달
  public static InvalidBookException withField(ErrorKey field, String reason) {
    InvalidBookException exception = new InvalidBookException();

    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.REASON, reason);

    return exception;
  }

  // 필드, 값, 이유를 함께 전달
  public static InvalidBookException withFieldAndValue(
      ErrorKey field, Object value, String reason) {
    InvalidBookException exception = new InvalidBookException();

    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.VALUE, value);
    exception.addDetail(ErrorKey.REASON, reason);
    
    return exception;
  }
}