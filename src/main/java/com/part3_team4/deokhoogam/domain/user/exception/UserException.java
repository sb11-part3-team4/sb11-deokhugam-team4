package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class UserException extends BusinessException {

  protected UserException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected UserException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}