package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import java.util.UUID;

public interface BookService {

  BookDto create(BookCreateRequest request);

  BookDto update(UUID id, BookUpdateRequest request);

  BookDto getDetails(UUID bookId);

  NaverBookDto getByIsbn(String isbn);


}