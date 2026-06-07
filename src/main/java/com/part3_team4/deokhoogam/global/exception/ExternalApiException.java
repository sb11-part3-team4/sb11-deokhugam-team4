package com.part3_team4.deokhoogam.global.exception;

public class ExternalApiException extends BusinessException {

  private ExternalApiException(ErrorCode errorCode) {
    super(errorCode);
  }

  public static ExternalApiException withIsbn(String isbn) {
    ExternalApiException exception = new ExternalApiException(ErrorCode.EXTERNAL_API_ERROR);
    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);
    return exception;
  }

  public static ExternalApiException from(ErrorCode errorCode) {
    return new ExternalApiException(errorCode);
  }

  public static ExternalApiException withCause(ErrorCode errorCode, Throwable cause) {
    ExternalApiException exception = new ExternalApiException(errorCode);
    exception.initCause(cause);
    return exception;
  }

  public static ExternalApiException withCause(ErrorCode errorCode, String detailMessage,
      Throwable cause) {
    ExternalApiException exception = new ExternalApiException(errorCode);
    exception.addDetail(ErrorKey.REASON, detailMessage);
    exception.initCause(cause);
    return exception;
  }
}