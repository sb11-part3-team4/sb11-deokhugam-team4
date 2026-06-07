package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import org.springframework.data.domain.Slice;

public interface BookRepositoryCustom {
  Slice<Book> getBooks(BookCursor cursor, BookGetListRequest request);
}