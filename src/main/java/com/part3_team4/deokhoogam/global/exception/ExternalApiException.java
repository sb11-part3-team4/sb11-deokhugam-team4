package com.part3_team4.deokhoogam.global.exception;

public class ExternalApiException extends BusinessException{

  public ExternalApiException() {
    super(ErrorCode.EXTERNAL_API_ERROR);
  }

  public static ExternalApiException withIsbn(String isbn) {
    ExternalApiException exception = new ExternalApiException();
    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);
    return exception;
  }

}
