package com.part3_team4.deokhoogam.domain.book.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class BookNotFoundException extends BookException {

  private BookNotFoundException() {
    super(ErrorCode.BOOK_NOT_FOUND);
  }

  public static BookNotFoundException withId(UUID bookId) {
    BookNotFoundException exception = new BookNotFoundException();
    exception.addDetail(ErrorKey.BOOK_ID, bookId);
    return exception;
  }

  public static BookNotFoundException withIsbn(String isbn) {
    BookNotFoundException exception = new BookNotFoundException();
    exception.addDetail(ErrorKey.BOOK_ISBN, isbn);
    return exception;
  }
}