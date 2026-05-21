package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.Map;

public class NotificationException extends BusinessException {

  public NotificationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

}
