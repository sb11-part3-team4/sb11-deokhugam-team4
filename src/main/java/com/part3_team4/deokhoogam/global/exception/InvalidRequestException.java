package com.part3_team4.deokhoogam.global.exception;

public class InvalidRequestException extends BusinessException {

  public InvalidRequestException(ErrorCode errorCode)
  {
    super(errorCode);
  }
}
