package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class NotificationException extends BusinessException {

  protected NotificationException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected NotificationException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}