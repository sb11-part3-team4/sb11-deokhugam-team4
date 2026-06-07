package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidDirectionException extends BookException {

  private InvalidDirectionException() {
    super(ErrorCode.INVALID_DIRECTION_VALUE);
  }

  public static InvalidDirectionException withValue(String value) {
    InvalidDirectionException exception = new InvalidDirectionException();
    exception.addDetail(ErrorKey.VALUE, value);
    return exception;
  }


}
