package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidIsbnException extends BookException {

  public InvalidIsbnException() {
    super(ErrorCode.INVALID_ISBN);
  }

  public static InvalidIsbnException withIsbn(String isbn) {
    InvalidIsbnException exception = new InvalidIsbnException();
    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);
    return exception;
  }





}
