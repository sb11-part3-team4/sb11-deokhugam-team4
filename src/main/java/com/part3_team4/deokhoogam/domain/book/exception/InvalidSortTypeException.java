package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidSortTypeException extends BookException {

  private InvalidSortTypeException() {

    super(ErrorCode.INVALID_SORT_TYPE_ERROR);
  }

  public static InvalidSortTypeException withValue(String value) {
    InvalidSortTypeException exception = new InvalidSortTypeException();
    exception.addDetail(ErrorKey.VALUE, value);
    return exception;
  }

}
