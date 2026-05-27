package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class CommentException extends BusinessException {

  protected CommentException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected CommentException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}