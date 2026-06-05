package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class OcrProcessingException extends BookException {

  private OcrProcessingException(ErrorCode errorCode) {
    super(errorCode);
  }

  public static OcrProcessingException from(ErrorCode errorCode) {
    return new OcrProcessingException(errorCode);
  }

  public static OcrProcessingException withDetail(ErrorCode errorCode, String detailMessage) {
    OcrProcessingException exception = new OcrProcessingException(errorCode);
    exception.addDetail(ErrorKey.DETAIL_MESSAGE, detailMessage);
    return exception;
  }
}