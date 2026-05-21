package com.part3_team4.deokhoogam.domain.user.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.Map;

public class UserException extends BusinessException {

  public UserException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

}
