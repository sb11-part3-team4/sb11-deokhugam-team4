package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  private final NaverApiService naverApiService;

  @Override
  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (bookRepository.existsByIsbn(request.isbn())) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }

    // 동시성 문제 방지
    try {
      Book book = Book.builder()
          .isbn(request.isbn())
          .title(request.title())
          .author(request.author())
          .description(request.description())
          .publisher(request.publisher())
          .publishedDate(request.publishedDate())
          .build();
      return BookDto.from(bookRepository.save(book));
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e;
      while (cause != null) {
        if (cause instanceof ConstraintViolationException cve
            && "uk_book_isbn".equalsIgnoreCase(cve.getConstraintName())) {
          throw IsbnAlreadyExistsException.withIsbn(request.isbn());
        }
        cause = cause.getCause();
      }
      throw e;
    }
  }


  @Transactional
  public BookDto update(UUID id, BookUpdateRequest request) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> BookNotFoundException.withId(id));

    book.updateBookInfo(
        request.title(),
        request.author(),
        request.description(),
        request.publisher(),
        request.publishedDate(),
        book.getThumbnailUrl() // TODO: 추후 메서드 분리 및 관련 로직 추가 예정
    );

    return BookDto.from(book);
  }


  @Override
  @Transactional(readOnly = true)
  public BookDto getDetails(UUID bookId) {

    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> BookNotFoundException.withId(bookId));

    return BookDto.from(book);
  }

  @Override
  public NaverBookDto getByIsbn(String isbn) {

    //isbn 형식 검사
    if (isbn == null || !isbn.matches("^[0-9]{13}$")) {
      throw InvalidIsbnException.withIsbn(isbn);
    }

    NaverBookDto response = naverApiService.getBookInfoByIsbn(isbn);

    if (response == null) {
      throw BookNotFoundException.withIsbn(isbn);
    }

    return response;

  }
}
