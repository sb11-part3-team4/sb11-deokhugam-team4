package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.instructure.naver.NaverApiService;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.DeletedBookRepository;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private static final String BOOK_THUMBNAIL_DIR = "books";
  private static final String ISBN_UNIQUE_CONSTRAINT = "uk_book_isbn";

  private final BookRepository bookRepository;
  private final DeletedBookRepository deletedBookRepository;

  private final FileUploader fileUploader;

  private final NaverApiService naverApiService;



  @Override
  @Transactional
  public BookDto create(BookCreateRequest request, MultipartFile thumbnailFile) {
    validateDuplicateIsbn(request.isbn());

    String thumbnailUrl = uploadThumbnail(thumbnailFile);

    // TODO: S3 업로드 후 DB 저장 실패 시 고아 파일 처리 정책

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
  public BookDto update(UUID id, BookUpdateRequest request, MultipartFile thumbnailFile) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> BookNotFoundException.withId(id));

    String newThumbnailUrl = uploadThumbnail(thumbnailFile);
    if (newThumbnailUrl != null) {
      // TODO: 기존 파일 삭제 정책
      book.updateThumbnail(newThumbnailUrl);
    }

    book.updateBookInfo(
        request.title(),
        request.author(),
        request.description(),
        request.publisher(),
        request.publishedDate()
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

  @Override
  @Transactional
  public void delete(UUID bookId) {

    //정보 가져온 후
    Book book = bookRepository.findById(bookId).orElseThrow(()->BookNotFoundException.withId(bookId));

    //삭제
    bookRepository.deleteById(bookId);

    //삭제 버전으로
    DeletedBook deletedBook = DeletedBook.from(book);

    //저장

    deletedBookRepository.save(deletedBook);

  }

  @Override
  @Transactional
  public void deleteHard(UUID bookId) {

    //존재 여부 체크
    if(!deletedBookRepository.existsById(bookId))
      throw BookNotFoundException.withId(bookId);
    //삭제
    deletedBookRepository.deleteById(bookId);

    //고아 파일 처리는 하위 도메인의 배치 연산으로 정리
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



