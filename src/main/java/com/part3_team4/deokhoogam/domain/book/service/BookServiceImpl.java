package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.BookDeletedEvent;
import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.naver.NaverApiService;
import com.part3_team4.deokhoogam.domain.book.repository.BookPersistence;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.DeletedBookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.OrphanThumbnailRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.InvalidRequestException;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
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
  private final DeletedBookRepository deletedBookRepository;
  private final OrphanThumbnailRepository orphanThumbnailRepository;

  private final FileUploader fileUploader;

  private final NaverApiService naverApiService;

  private final BookPersistence bookPersistence;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public BookDto create(BookCreateRequest request, MultipartFile thumbnailFile) {
    validateDuplicateIsbn(request.isbn());

    String thumbnailUrl = uploadThumbnail(thumbnailFile);
    String originalFilename = extractOriginalFilename(thumbnailFile);

    try {
      Book book = Book.builder()
          .isbn(request.isbn())
          .title(request.title())
          .author(request.author())
          .description(request.description())
          .publisher(request.publisher())
          .publishedDate(request.publishedDate())
          .thumbnailUrl(thumbnailUrl)
          .originalFilename(originalFilename)
          .build();

      Book saved = bookPersistence.save(book);
      log.info("도서 등록 완료: bookId={}, isbn={}", saved.getId(), saved.getIsbn());
      return BookDto.from(saved);

    } catch (DataIntegrityViolationException e) {
      if (thumbnailUrl != null) {
        log.warn("도서 등록 실패로 업로드된 썸네일 롤백: url={}", thumbnailUrl);
        fileUploader.delete(thumbnailUrl);
      }

      if (isDuplicateIsbnViolation(e)) {
        throw IsbnAlreadyExistsException.withIsbn(request.isbn());
      }
      throw e;

    } catch (Exception e) {
      if (thumbnailUrl != null) {
        log.warn("도서 등록 중 예외 발생으로 업로드된 썸네일 롤백: url={}", thumbnailUrl);
        fileUploader.delete(thumbnailUrl);
      }
      throw e;
    }
  }

  @Override
  public BookDto update(UUID id, BookUpdateRequest request, MultipartFile thumbnailFile) {
    Book oldBook = bookRepository.findById(id).orElseThrow(() -> BookNotFoundException.withId(id));
    String oldThumbnailUrl = oldBook.getThumbnailUrl();

    String newThumbnailUrl = uploadThumbnail(thumbnailFile);
    String originalFilename = extractOriginalFilename(thumbnailFile);

    try {
      Book updatedBook = bookPersistence.update(id, request, newThumbnailUrl, originalFilename);

      if (newThumbnailUrl != null && oldThumbnailUrl != null
          && !oldThumbnailUrl.equals(newThumbnailUrl)) {
        orphanThumbnailRepository.save(new OrphanThumbnail(oldThumbnailUrl));
        log.info("고아 썸네일 등록: {}", oldThumbnailUrl);
      }

      log.info("도서 수정 완료: bookId={}", id);
      return BookDto.from(updatedBook);

    } catch (Exception e) {
      if (newThumbnailUrl != null) {
        log.warn("도서 수정 실패로 업로드된 썸네일 롤백: bookId={}, url={}", id, newThumbnailUrl);
        fileUploader.delete(newThumbnailUrl);
      }
      throw e;
    }
  }

  @Transactional
  @Override
  public void updateReviewData(UUID bookId, int reviewCount, BigDecimal rating) {
    bookPersistence.updateReviewData(bookId, reviewCount, rating);
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
        generateNextCursor(books, request.orderBy()),
        books.hasNext() && !books.getContent().isEmpty()
            ? books.getContent().get(books.getContent().size() - 1).getCreatedAt().toString()
            : null,
        books.getSize(),
        null,
        books.hasNext()
    );
  }

  //다음 커서 만들기
  private String generateNextCursor(Slice<Book> books, SortType sortType) {
    if (!books.hasNext() || books.getContent().isEmpty()) {
      return null; // 다음 페이지가 없으면 null 반환
    }

    // 현재 슬라이스의 마지막 데이터 추출
    Book lastBook = books.getContent().get(books.getContent().size() - 1);

    //커서 메인 값 정하기
    String mainValue = switch (sortType) {
      case TITLE -> lastBook.getTitle();
      case RATING -> lastBook.getRating().toString();
      case PUBLISHED_DATE -> lastBook.getPublishedDate().toString();
      case REVIEW_COUNT -> String.valueOf(lastBook.getReviewCount());
    };

    // 마지막 데이터의 값으로 다음 커서 객체 생성
    BookCursor newCursorObj = new BookCursor(
        mainValue,
        lastBook.getId(),
        lastBook.getCreatedAt()
    );

    return CursorUtils.encodeCursor(newCursorObj);
  }

  @Override
  @Transactional
  public void delete(UUID bookId) {

    //정보 가져온 후
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> BookNotFoundException.withId(bookId));

    //삭제 전 이벤트 발행
    eventPublisher.publishEvent(new BookDeletedEvent(bookId, false));

    //삭제
    bookRepository.deleteById(bookId);

    //삭제 버전으로
    DeletedBook deletedBook = DeletedBook.from(book);

    //저장

    deletedBookRepository.save(deletedBook);

    log.info("도서 논리 삭제 완료: bookId={}", bookId);

  }

  @Override
  @Transactional
  public void deleteHard(UUID bookId) {

    //S3 삭제 및 URL 가져오기
    DeletedBook deletedBook = deletedBookRepository.findById(bookId)
        .orElseThrow(() -> BookNotFoundException.withId(bookId));

    //물리 삭제 전 이벤트 발행
    eventPublisher.publishEvent(new BookDeletedEvent(bookId, true));

    deletedBookRepository.deleteById(bookId);

    fileUploader.delete(deletedBook.getThumbnailUrl());

    //고아 파일 처리는 하위 도메인의 배치 연산으로 정리
    log.info("도서 물리 삭제 완료: bookId={}, thumbnailUrl={}", bookId, deletedBook.getThumbnailUrl());
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

  private String extractOriginalFilename(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    return file.getOriginalFilename();
  }

}



