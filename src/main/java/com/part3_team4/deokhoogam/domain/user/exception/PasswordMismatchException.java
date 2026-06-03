package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class PasswordMismatchException extends UserException{
  public PasswordMismatchException() {
    super(ErrorCode.PASSWORD_MISMATCH);
  }
}
