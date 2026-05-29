package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class UserAlreadyExistsException extends UserException{
  private UserAlreadyExistsException() {
    super(ErrorCode.USER_ALREADY_EXISTS);
  }

  public static UserAlreadyExistsException withEmail(String email) {
    UserAlreadyExistsException exception = new UserAlreadyExistsException();
    exception.addDetail(ErrorKey.REASON, email);
    return exception;
  }

  public static UserAlreadyExistsException withName(String name) {
    UserAlreadyExistsException exception = new UserAlreadyExistsException();
    exception.addDetail(ErrorKey.REASON, name);
    return exception;
  }
}
