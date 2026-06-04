package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class OcrProcessingException extends BookException {

  private OcrProcessingException() {
    super(ErrorCode.OCR_PROCESSING_FAILED);
  }

  public static OcrProcessingException withDetail(String detailMessage) {
    OcrProcessingException exception = new OcrProcessingException();
    exception.addDetail(ErrorKey.DETAIL_MESSAGE, detailMessage);
    return exception;
  }
}