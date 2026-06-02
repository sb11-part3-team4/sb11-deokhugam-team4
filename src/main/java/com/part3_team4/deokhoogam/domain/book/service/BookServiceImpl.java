package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private static final String BOOK_THUMBNAIL_DIR = "books";
  private static final String ISBN_UNIQUE_CONSTRAINT = "uk_book_isbn";

  private final BookRepository bookRepository;
  private final FileUploader fileUploader;

  /// TODO: S3 업로드 후 DB 저장 실패 시 orphan file 정리 필요
  @Override
  @Transactional
  public BookDto create(BookCreateRequest request, MultipartFile thumbnailFile) {
    validateDuplicateIsbn(request.isbn());

    String thumbnailUrl = uploadThumbnail(thumbnailFile);

    try {
      Book book = Book.builder()
          .isbn(request.isbn())
          .title(request.title())
          .author(request.author())
          .description(request.description())
          .publisher(request.publisher())
          .publishedDate(request.publishedDate())
          .thumbnailUrl(thumbnailUrl)
          .build();

      return BookDto.from(bookRepository.save(book));
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateIsbnViolation(e)) {
        throw IsbnAlreadyExistsException.withIsbn(request.isbn());
      }
      throw e;
    }
  }

  @Override
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

  private void validateDuplicateIsbn(String isbn) {
    if (bookRepository.existsByIsbn(isbn)) {
      throw IsbnAlreadyExistsException.withIsbn(isbn);
    }
  }

  private String uploadThumbnail(MultipartFile thumbnailFile) {
    if (thumbnailFile == null || thumbnailFile.isEmpty()) {
      return null;
    }
    return fileUploader.upload(thumbnailFile, BOOK_THUMBNAIL_DIR);
  }

  private boolean isDuplicateIsbnViolation(DataIntegrityViolationException e) {
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException cve
          && ISBN_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName())) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}