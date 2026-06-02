package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

  @InjectMocks
  private BookServiceImpl bookService;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private FileUploader fileUploader;

  private static final String BOOK_THUMBNAIL_DIR = "books";
  private static final String ISBN_UNIQUE_CONSTRAINT = "uk_book_isbn";

  @Test
  @DisplayName("새로운 도서를 성공적으로 등록한다")
  void createBook_Success() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    UUID mockId = UUID.randomUUID();

    Book mockSavedBook = Book.builder()
        .isbn(request.isbn())
        .title(request.title())
        .author(request.author())
        .description(request.description())
        .publisher(request.publisher())
        .publishedDate(request.publishedDate())
        .build();

    setField(mockSavedBook, "id", mockId);

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);
    given(bookRepository.save(any(Book.class))).willReturn(mockSavedBook);

    // when
    BookDto result = bookService.create(request, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(mockId);
    assertThat(result.isbn()).isEqualTo(request.isbn());
    assertThat(result.title()).isEqualTo(request.title());

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    then(bookRepository).should().save(bookCaptor.capture());

    Book actualSavedBook = bookCaptor.getValue();
    assertThat(actualSavedBook.getIsbn()).isEqualTo(request.isbn());
    assertThat(actualSavedBook.getTitle()).isEqualTo(request.title());
    assertThat(actualSavedBook.getAuthor()).isEqualTo(request.author());
  }

  @Test
  @DisplayName("도서 등록 시 썸네일 파일이 주어지면 파일 저장소에 업로드 후 반환된 URL을 포함하여 저장한다")
  void createBook_WithThumbnail_Success() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile thumbnailFile = new MockMultipartFile(
        "thumbnailFile",
        "image.png",
        "image/png",
        "test_content".getBytes());

    String mockUploadedUrl = "https://s3.deokhugam.com/books/test-image.png";
    UUID mockId = UUID.randomUUID();

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);
    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR))).willReturn(
        mockUploadedUrl);

    Book mockSavedBook = Book.builder()
        .isbn(request.isbn())
        .title(request.title())
        .author(request.author())
        .description(request.description())
        .publisher(request.publisher())
        .publishedDate(request.publishedDate())
        .thumbnailUrl(mockUploadedUrl)
        .build();
    setField(mockSavedBook, "id", mockId);

    given(bookRepository.save(any(Book.class))).willReturn(mockSavedBook);

    // when
    BookDto result = bookService.create(request, thumbnailFile);

    // then
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo(mockUploadedUrl);

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    then(bookRepository).should().save(bookCaptor.capture());
    assertThat(bookCaptor.getValue().getThumbnailUrl()).isEqualTo(mockUploadedUrl);
  }

  @Test
  @DisplayName("파일 스토리지 업로드 중 예외가 발생하면 도서 등록이 실패하고 예외가 상위로 전파된다")
  void createBook_ThumbnailUploadFail_ThrowsException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile thumbnailFile = new MockMultipartFile(
        "thumbnailFile",
        "image.png",
        "image/png",
        "test_content".getBytes());

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);
    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR)))
        .willThrow(new RuntimeException("S3 업로드 에러 발생"));

    // when & then
    assertThatThrownBy(() -> bookService.create(request, thumbnailFile))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("S3 업로드 에러 발생");

    then(bookRepository).should(never()).save(any(Book.class));
  }

  @Test
  @DisplayName("이미 존재하는 ISBN으로 도서를 등록하면 예외가 발생한다")
  void createBook_WithAlreadyExistIsbn_ThrowsException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(IsbnAlreadyExistsException.class);

    then(bookRepository).should().existsByIsbn(request.isbn());
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("ISBN 제약 위반시 IsbnAlreadyExistsException으로 변환한다")
  void createBook_UniqueViolationOnIsbn_ThrowsBusinessException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    DataIntegrityViolationException exception = createDataIntegrityViolation(
        ISBN_UNIQUE_CONSTRAINT);
    given(bookRepository.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(IsbnAlreadyExistsException.class);
  }

  @Test
  @DisplayName("ISBN 외 제약 위반 발생 시 원본 예외 그대로 전파한다")
  void createBook_DataIntegrityViolationOnOtherConstraint_Propagates() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    DataIntegrityViolationException exception = createDataIntegrityViolation("other_constraint");
    given(bookRepository.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("원인이 ConstraintViolationException이 아닌 DataIntegrityViolationException 발생 시 원본 예외를 전파한다")
  void createBook_DataIntegrityViolationWithDifferentCause_Propagates() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    SQLException differentCause = new SQLException("일반적인 SQL 문법 에러 등");
    DataIntegrityViolationException exception = new DataIntegrityViolationException("예외 발생",
        differentCause);

    given(bookRepository.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("기존 도서 정보를 요청에 따라 수정한다")
  void updateBook_Success() {
    // given
    UUID targetId = UUID.randomUUID();
    BookCreateRequest createRequest = BookFixtures.validBookCreateRequest();
    BookUpdateRequest updateRequest = BookFixtures.validBookUpdateRequest();

    Book existingBook = Book.builder()
        .isbn(createRequest.isbn())
        .title(createRequest.title())
        .author(createRequest.author())
        .description(createRequest.description())
        .publisher(createRequest.publisher())
        .publishedDate(createRequest.publishedDate())
        .build();

    setField(existingBook, "id", targetId);

    given(bookRepository.findById(targetId)).willReturn(Optional.of(existingBook));

    // when
    BookDto result = bookService.update(targetId, updateRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(targetId);
    assertThat(result.title()).isEqualTo(updateRequest.title());
    assertThat(result.author()).isEqualTo(updateRequest.author());

    assertThat(existingBook.getTitle()).isEqualTo(updateRequest.title());
    assertThat(existingBook.getAuthor()).isEqualTo(updateRequest.author());
    assertThat(existingBook.getDescription()).isEqualTo(updateRequest.description());
    assertThat(existingBook.getPublisher()).isEqualTo(updateRequest.publisher());
    assertThat(existingBook.getPublishedDate()).isEqualTo(updateRequest.publishedDate());

    then(bookRepository).should().findById(targetId);
    then(bookRepository).should(never()).save(any(Book.class));
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 수정을 요청하면 예외가 발생한다")
  void updateBook_WithNonExistentId_ThrowsException() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    given(bookRepository.findById(nonExistentId)).willReturn(
        Optional.empty());

    // when & then
    assertThatThrownBy(() -> bookService.update(nonExistentId, request))
        .isInstanceOf(BookNotFoundException.class);

    then(bookRepository).should().findById(nonExistentId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  private DataIntegrityViolationException createDataIntegrityViolation(String constraintName) {
    ConstraintViolationException cause = new ConstraintViolationException(
        "제약조건명 검증",
        new SQLException(),
        constraintName);
    return new DataIntegrityViolationException("데이터 무결성 예외 발생", cause);
  }
}