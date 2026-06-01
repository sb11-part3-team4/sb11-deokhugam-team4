package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.util.UUID;

public interface BookService {

  BookDto create(BookCreateRequest request);

  BookDto getDetails(UUID bookId);

  PageResponse<BookDto> getBooks(BookGetListRequest request);
}