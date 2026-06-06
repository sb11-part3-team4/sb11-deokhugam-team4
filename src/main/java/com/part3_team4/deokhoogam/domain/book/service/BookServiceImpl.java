package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.instructure.naver.NaverApiService;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.InvalidRequestException;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
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
  public PageResponse<BookDto> getBooks(BookGetListRequest request) {

    if (request.limit() < 1 || request.limit() > 100) {
      throw new InvalidRequestException(ErrorCode.INVALID_INPUT_VALUE);
    }

    //커서 디코딩
    BookCursor cursor = CursorUtils.decodeCursor(request.cursor());
    //슬라이스 가져오기
    Slice<Book> books = bookRepository.getBooks(cursor, request);
    //Dto 변환
    List<BookDto> dtoList = books.stream().map(BookDto::from).toList();

    return new PageResponse<>(
        dtoList,
        generateNextCursor(books),
        books.hasNext() && !books.getContent().isEmpty()
            ? books.getContent().get(books.getContent().size() - 1).getCreatedAt().toString()
            : null,
        books.getSize(),
        null,
        books.hasNext()
    );
  }

  //다음 커서 만들기
  private String generateNextCursor(Slice<Book> books) {
    if (!books.hasNext() || books.getContent().isEmpty()) {
      return null; // 다음 페이지가 없으면 null 반환
    }

    // 현재 슬라이스의 마지막 데이터 추출
    Book lastBook = books.getContent().get(books.getContent().size() - 1);

    // 마지막 데이터의 값으로 다음 커서 객체 생성
    BookCursor newCursorObj = new BookCursor(
        lastBook.getTitle(),
        lastBook.getId(),
        lastBook.getCreatedAt()
    );

    return CursorUtils.encodeCursor(newCursorObj);
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
