package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.Map;

public class CommentException extends BusinessException {

  public CommentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

}
