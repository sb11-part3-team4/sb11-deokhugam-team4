package com.part3_team4.deokhoogam.domain.review.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.Map;

public class ReviewException extends BusinessException {

  public ReviewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

}
