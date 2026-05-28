package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (bookRepository.existsByIsbn(request.isbn())) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }

    Book book = request.toEntity();
    return BookDto.from(bookRepository.save(book));
  }
}