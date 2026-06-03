package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class UserAlreadyExistsException extends UserException{
  private UserAlreadyExistsException() {
    super(ErrorCode.USER_ALREADY_EXISTS);
  }

  public static UserAlreadyExistsException withEmail() {
    return new UserAlreadyExistsException();
  }

  public static UserAlreadyExistsException withName() {
    return new UserAlreadyExistsException();
  }
}
