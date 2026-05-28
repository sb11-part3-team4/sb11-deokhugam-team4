package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class IsbnAlreadyExistsException extends BookException {

  private IsbnAlreadyExistsException() {
    super(ErrorCode.ISBN_ALREADY_EXISTS);
  }

  public static IsbnAlreadyExistsException withIsbn(String isbn) {
    IsbnAlreadyExistsException exception = new IsbnAlreadyExistsException();

    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);

    return exception;
  }
}