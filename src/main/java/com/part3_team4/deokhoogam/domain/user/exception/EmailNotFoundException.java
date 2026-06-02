package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class EmailNotFoundException extends UserException{
  public EmailNotFoundException() {
    super(ErrorCode.EMAIL_NOT_FOUND);
  }
}
