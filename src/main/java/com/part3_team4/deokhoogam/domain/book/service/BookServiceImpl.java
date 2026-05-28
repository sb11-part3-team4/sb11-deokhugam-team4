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
public class BookServiceImpl {

  private final BookRepository bookRepository;

  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (bookRepository.existsByIsbn(request.getIsbn())) {
      throw IsbnAlreadyExistsException.withIsbn(request.getIsbn());
    }

    Book book = Book.builder()
        .isbn(request.getIsbn())
        .title(request.getTitle())
        .author(request.getAuthor())
        .description(request.getDescription())
        .publisher(request.getPublisher())
        .publishedDate(request.getPublishedDate())
        .build();

    Book savedBook = bookRepository.save(book);
    return BookDto.from(savedBook);
  }
}