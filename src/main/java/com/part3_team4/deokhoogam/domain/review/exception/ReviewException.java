package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class ReviewException extends BusinessException {

  protected ReviewException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected ReviewException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}