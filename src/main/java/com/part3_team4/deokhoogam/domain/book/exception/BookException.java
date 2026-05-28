package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class BookException extends BusinessException {

  protected BookException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected BookException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}