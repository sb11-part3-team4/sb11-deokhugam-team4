package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  @Override
  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (bookRepository.existsByIsbn(request.isbn())) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }

    // 동시성 문제 방지
    try {
      Book book = request.toEntity();
      return BookDto.from(bookRepository.save(book));
    } catch (DataIntegrityViolationException e) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }
  }

  @Override
  @Transactional
  public BookDto update(UUID id, BookUpdateRequest request) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> BookNotFoundException.withId(id));

    book.updateBookInfo(
        request.getTitle(),
        request.getAuthor(),
        request.getDescription(),
        request.getPublisher(),
        request.getPublishedDate(),
        book.getThumbnailUrl() // TODO: 추후 메서드 분리 및 관련 로직 추가 예정
    );

    return BookDto.from(book);
  }
}