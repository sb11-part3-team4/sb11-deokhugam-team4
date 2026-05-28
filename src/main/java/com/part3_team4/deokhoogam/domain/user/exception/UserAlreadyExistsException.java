package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class UserAlreadyExistsException extends UserException{
  private UserAlreadyExistsException(String message) {
    super(ErrorCode.USER_ALREADY_EXISTS);
  }

  public static UserAlreadyExistsException withEmail(String email) {
    UserAlreadyExistsException exception = new UserAlreadyExistsException("이미 존재하는 이메일입니다.");
    exception.addDetail(ErrorKey.REASON, email);
    return exception;
  }

  public static UserAlreadyExistsException withName(String name) {
    UserAlreadyExistsException exception = new UserAlreadyExistsException("이미 존재하는 이름입니다.");
    exception.addDetail(ErrorKey.REASON, name);
    return exception;
  }
}
