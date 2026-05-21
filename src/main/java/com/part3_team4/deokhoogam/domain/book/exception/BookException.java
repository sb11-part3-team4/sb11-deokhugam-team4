package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.Map;

public class BookException extends BusinessException {

  public BookException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

}
