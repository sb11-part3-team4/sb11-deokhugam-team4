package com.part3_team4.deokhoogam.global.exception;

public class ExternalApiException extends BusinessException{

  public ExternalApiException() {
    super(ErrorCode.INVALID_INPUT_VALUE);
  }

  public static ExternalApiException withIsbn(String isbn) {
    ExternalApiException exception = new ExternalApiException();
    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);
    return exception;
  }

}
