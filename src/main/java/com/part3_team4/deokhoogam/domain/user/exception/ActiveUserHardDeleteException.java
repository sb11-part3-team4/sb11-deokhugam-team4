package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class ActiveUserHardDeleteException extends UserException {

  public ActiveUserHardDeleteException() {
    super(ErrorCode.USER_NOT_DELETED_YET);
  }
}