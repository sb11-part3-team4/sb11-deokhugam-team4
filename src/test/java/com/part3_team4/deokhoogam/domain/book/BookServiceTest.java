package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.naver.NaverApiService;
import com.part3_team4.deokhoogam.domain.book.repository.BookPersistence;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.DeletedBookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.Base64Exception;
import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.fixture.BookFixtureFactory;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.fixture.NaverBookFixture;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

  @InjectMocks
  private BookServiceImpl bookService;

  @Mock
  private NaverApiService naverApiService;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private DeletedBookRepository deletedBookRepository;

  @Mock
  private FileUploader fileUploader;

  @Mock
  private BookPersistence bookPersistence;

  @Mock
  private ApplicationEventPublisher eventPublisher;

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
    given(bookPersistence.save(any(Book.class))).willReturn(mockSavedBook);

    // when
    BookDto result = bookService.create(request, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(mockId);
    assertThat(result.isbn()).isEqualTo(request.isbn());
    assertThat(result.title()).isEqualTo(request.title());

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    then(bookPersistence).should().save(bookCaptor.capture());

    Book actualSavedBook = bookCaptor.getValue();
    assertThat(actualSavedBook.getIsbn()).isEqualTo(request.isbn());
    assertThat(actualSavedBook.getTitle()).isEqualTo(request.title());
    assertThat(actualSavedBook.getAuthor()).isEqualTo(request.author());
  }

  @Test
  @DisplayName("도서 등록 시 썸네일 파일이 주어지면 파일 저장소에 업로드 후 원본 파일명도 함께 저장한다")
  void createBook_WithThumbnail_Success() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    String originalFilename = "image.png";
    MockMultipartFile thumbnailFile = new MockMultipartFile(
        "thumbnailFile",
        originalFilename,
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
        .originalFilename(originalFilename)
        .build();
    setField(mockSavedBook, "id", mockId);

    given(bookPersistence.save(any(Book.class))).willReturn(mockSavedBook);

    // when
    BookDto result = bookService.create(request, thumbnailFile);

    // then
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo(mockUploadedUrl);

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    then(bookPersistence).should().save(bookCaptor.capture());
    assertThat(bookCaptor.getValue().getThumbnailUrl()).isEqualTo(mockUploadedUrl);
    assertThat(bookCaptor.getValue().getOriginalFilename()).isEqualTo(originalFilename);
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

    then(bookPersistence).shouldHaveNoInteractions();
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
    then(bookPersistence).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("ISBN 제약 위반시 IsbnAlreadyExistsException으로 변환한다")
  void createBook_UniqueViolationOnIsbn_ThrowsBusinessException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    DataIntegrityViolationException exception = createDataIntegrityViolation(
        ISBN_UNIQUE_CONSTRAINT);
    given(bookPersistence.save(any(Book.class))).willThrow(exception);

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
    given(bookPersistence.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("도서 생성 중 ISBN 제약조건 위반 예외가 발생하면 업로드된 S3 파일을 삭제하고 IsbnAlreadyExistsException을 던진다")
  void createBook_DataIntegrityViolation_DeletesFileAndThrowsException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile file = new MockMultipartFile(
        "thumbnailImage",
        "test.png",
        "image/png",
        "content".getBytes());
    String uploadedUrl = "https://s3.com/books/test.png";

    given(fileUploader.upload(any(), any())).willReturn(uploadedUrl);

    DataIntegrityViolationException exception = createDataIntegrityViolation(
        ISBN_UNIQUE_CONSTRAINT);
    given(bookPersistence.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, file)).isInstanceOf(
        IsbnAlreadyExistsException.class);

    then(fileUploader).should().delete(uploadedUrl);
  }

  @Test
  @DisplayName("도서 생성 중 일반 예외가 발생하면 업로드된 S3 파일을 정리하고 예외를 전파한다")
  void createBook_GeneralException_DeletesFileAndPropagates() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile file = new MockMultipartFile(
        "thumbnailImage",
        "test.png",
        "image/png",
        "content".getBytes());
    String uploadedUrl = "https://s3.com/books/test.png";

    given(fileUploader.upload(any(), any())).willReturn(uploadedUrl);

    given(bookPersistence.save(any(Book.class)))
        .willThrow(new RuntimeException("DB 오류 발생"));

    // when & then
    assertThatThrownBy(() -> bookService.create(request, file))
        .isInstanceOf(RuntimeException.class);

    then(fileUploader).should().delete(uploadedUrl);
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

    given(bookPersistence.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> bookService.create(request, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("썸네일 없이 기존 도서 정보를 요청에 따라 수정한다")
  void updateBook_Success() {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest updateRequest = BookFixtures.validBookUpdateRequest();

    Book updatedBook = Book.builder()
        .isbn("1234567890123")
        .title(updateRequest.title())
        .author(updateRequest.author())
        .description(updateRequest.description())
        .publisher(updateRequest.publisher())
        .publishedDate(updateRequest.publishedDate())
        .build();
    setField(updatedBook, "id", targetId);

    given(bookPersistence.update(eq(targetId), eq(updateRequest), eq(null), eq(null)))
        .willReturn(updatedBook);

    // when
    BookDto result = bookService.update(targetId, updateRequest, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(targetId);
    assertThat(result.title()).isEqualTo(updateRequest.title());
    assertThat(result.author()).isEqualTo(updateRequest.author());

    then(bookPersistence).should().update(eq(targetId), eq(updateRequest), eq(null), eq(null));
  }

  @Test
  @DisplayName("도서 정보 수정 시 썸네일 파일이 주어지면 파일 업로드 후 URL과 원본 파일명을 업데이트한다")
  void updateBook_WithThumbnail_Success() {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    String newOriginalFilename = "image.png";
    MockMultipartFile thumbnailFile = new MockMultipartFile(
        "thumbnailFile",
        newOriginalFilename,
        "image/png",
        "test_content".getBytes());
    String newMockUrl = "https://s3.deokhugam.com/books/new-image.png";

    Book updatedBook = BookFixtures.validBook("1234567890123");
    setField(updatedBook, "id", targetId);
    ReflectionTestUtils.setField(updatedBook, "thumbnailUrl", newMockUrl);
    ReflectionTestUtils.setField(updatedBook, "originalFilename", newOriginalFilename);

    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR))).willReturn(
        newMockUrl);
    given(bookPersistence.update(eq(targetId), eq(request), eq(newMockUrl),
        eq(newOriginalFilename))).willReturn(updatedBook);

    // when
    BookDto result = bookService.update(targetId, request, thumbnailFile);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(newMockUrl);

    then(fileUploader).should(times(1)).upload(any(), eq(BOOK_THUMBNAIL_DIR));
    then(bookPersistence).should()
        .update(eq(targetId), eq(request), eq(newMockUrl), eq(newOriginalFilename));
  }

  @Test
  @DisplayName("도서 수정 시 파일 스토리지 업로드 중 예외가 발생하면 수정이 실패하고 예외가 전파된다")
  void updateBook_ThumbnailUploadFail_ThrowsException() {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    MockMultipartFile thumbnailFile = new MockMultipartFile(
        "thumbnailFile", "image.png", "image/png", "test_content".getBytes());

    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR)))
        .willThrow(new RuntimeException("S3 업로드 에러 발생"));

    // when & then
    assertThatThrownBy(() -> bookService.update(targetId, request, thumbnailFile))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("S3 업로드 에러 발생");

    then(bookPersistence).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("도서 수정 중 예외가 발생하면 새로 업로드된 S3 파일을 삭제하고 예외를 전파한다")
  void updateBook_Exception_DeletesNewFileAndPropagates() {
    // given
    UUID bookId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    String newOriginalFilename = "new.png";
    MockMultipartFile newFile = new MockMultipartFile(
        "thumbnailImage",
        newOriginalFilename,
        "image/png",
        "new-content".getBytes());
    String newUploadedUrl = "https://s3.com/books/new.png";

    given(fileUploader.upload(any(), any())).willReturn(newUploadedUrl);
    given(bookPersistence.update(eq(bookId), any(), eq(newUploadedUrl), eq(newOriginalFilename)))
        .willThrow(new RuntimeException("Update Failed"));

    // when & then
    assertThatThrownBy(() -> bookService.update(bookId, request, newFile))
        .isInstanceOf(RuntimeException.class);

    then(fileUploader).should().delete(newUploadedUrl);
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 수정을 요청하면 예외가 발생한다")
  void updateBook_WithNonExistentId_ThrowsException() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    given(bookPersistence.update(eq(nonExistentId), eq(request), eq(null), eq(null)))
        .willThrow(BookNotFoundException.withId(nonExistentId));

    // when & then
    assertThatThrownBy(() -> bookService.update(nonExistentId, request, null))
        .isInstanceOf(BookNotFoundException.class);

    then(bookPersistence).should().update(eq(nonExistentId), eq(request), eq(null), eq(null));
  }

  @Test
  @DisplayName("정상적인 데이터가 들어오면 Persistence에 수정을 위임한다")
  void updateReviewData_Success() {
    // given
    UUID mockId = UUID.randomUUID();
    int newCount = 10;
    BigDecimal newRating = new BigDecimal("4.50");

    // when
    bookService.updateReviewData(mockId, newCount, newRating);

    // then
    then(bookPersistence).should().updateReviewData(mockId, newCount, newRating);
  }

  @Nested
  @DisplayName("getDetails() 메서드")
  class TestGetDetails {

    UUID mockId = UUID.randomUUID();


    @Test
    @DisplayName("유효한 id가 들어왔을때 책 정보를 반환한다.")
    void returnDetails() {

      //given
      given(bookRepository.findById(mockId)).willReturn(book());

      //when
      BookDto result = bookService.getDetails(mockId);

      assertThat(result.id()).isEqualTo(mockId);
      assertThat(result.title()).isEqualTo("모비 딕");

      then(bookRepository).should().findById(mockId);


    }


    @Test
    @DisplayName("유효하지 않은 id가 들어왔을때 예외를 발생시킨다.")
    void returnDetailsInvalidId() {

      //given
      given(bookRepository.findById(mockId)).willReturn(Optional.empty());
      //then & when
      assertThatThrownBy(() -> bookService.getDetails(mockId)).isInstanceOf(
          BookNotFoundException.class);

    }


    private Optional<Book> book() {

      Book book = Book.builder()
          .title("모비 딕")
          .author("허먼 멜빌")
          .description("『모비 딕』 완역본")
          .publisher("작가정신")
          .publishedDate(LocalDate.of(2024, 4, 9))
          .thumbnailUrl("temp/url")
          .isbn("9791160263404")
          .build();

      ReflectionTestUtils.setField(book, "id", mockId);

      return Optional.of(book);

    }

  }


  @Nested
  @DisplayName("isbn으로 네이버 API를 통해 도서 정보를 가져올때")
  class TestGetBookByIsbn {

    @Test
    @DisplayName("유효한 isbn이면 도서 정보를 가져오고 200을 리턴한다")
    void return_200_and_data_when_valid_isbn() {
      // given
      String validIsbn = "9788957272541";
      given(naverApiService.getBookInfoByIsbn(validIsbn)).willReturn(
          NaverBookFixture.createValidNaverBookDto(validIsbn));

      // when
      NaverBookDto result = bookService.getByIsbn(validIsbn);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("모비 딕");
      assertThat(result.getPublisher()).isEqualTo("작가정신");
    }

    @Test
    @DisplayName("ISBN 형식에 맞지 않는 입력시 400 과 예외를 발생시킨다")
    void return_400_when_invalid_isbn() {

      String invalidIsbn = "978895727254";

      // when & then
      assertThatThrownBy(() -> bookService.getByIsbn(invalidIsbn)).isInstanceOf(
          InvalidIsbnException.class);
      then(naverApiService).shouldHaveNoMoreInteractions();

    }

    @Test
    @DisplayName("네이버 API에 없는 책이라면 404 예외를 발생시킨다")
    void return_404_when_book_not_found_by_isbn() {

      String notFoundIsbn = "9788957272542";
      given(naverApiService.getBookInfoByIsbn(notFoundIsbn)).willReturn(null);

      assertThatThrownBy(() -> bookService.getByIsbn(notFoundIsbn)).isInstanceOf(
          BookNotFoundException.class);
      then(naverApiService).should().getBookInfoByIsbn(notFoundIsbn);
      then(naverApiService).shouldHaveNoMoreInteractions();

    }


  }

  @Nested
  @DisplayName("도서 목록 조회 메서드에서")
  class TestGetBooks {

    @Nested
    @DisplayName("정상적인 데이터가 들어왔을때")
    class TestGetBooks_ValidData {

      @Test
      @DisplayName("기본 데이터가 들어오면 목록과 커서 관련 데이터가 담긴 응답을 리턴")
      void return_list_and_metadata_when_valid_data() {

        BookGetListRequest request = BookGetListRequest.builder()
            .limit(50)
            .build();

        List<Book> mockBooks = BookFixtureFactory.createBookList();
        Slice<Book> mockSlice = new SliceImpl<>(mockBooks);

        given(bookRepository.getBooks(any(), any())).willReturn(mockSlice);

        //when
        PageResponse<BookDto> result = bookService.getBooks(request);

        //then
        assertThat(result.content()).hasSize(mockBooks.size());
        assertThat(result.content().get(0).id()).isEqualTo(mockBooks.get(0).getId());

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();

        verify(bookRepository, times(1)).getBooks(any(), any());
      }


      @Test
      @DisplayName("limit 크기가 하나면 다음 커서와 hasnext = true를 반환한다")
      void return_list_and_metadata_when_valid_data_with_limit_one() {

        //given
        BookGetListRequest request = BookGetListRequest.builder()
            .limit(1)
            .build();

        List<Book> mockBooks = List.of(BookFixtureFactory.createBook1());
        //임시로 createdAt 필드 채워주기
        ReflectionTestUtils.setField(mockBooks.get(0), "createdAt", Instant.now());

        Pageable pageable = PageRequest.of(0, request.limit());
        Slice<Book> mockSlice = new SliceImpl<>(mockBooks, pageable, true);

        //when
        given(bookRepository.getBooks(any(), any())).willReturn(mockSlice);
        PageResponse<BookDto> result = bookService.getBooks(request);

        //then

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(mockBooks.get(0).getId());

        assertThat(result.hasNext()).isTrue();

        assertThat(result.nextCursor()).isNotNull();

      }

      @Test
      @DisplayName("조회된 데이터가 없으면 빈 리스트와 hasNext=false를 리턴한다")
      void return_empty_list_when_no_data() {
        // given
        BookGetListRequest request = BookGetListRequest.builder()
            .limit(50)
            .build();

        Slice<Book> mockSlice = new SliceImpl<>(List.of());

        given(bookRepository.getBooks(any(), any())).willReturn(mockSlice);

        // when
        PageResponse<BookDto> result = bookService.getBooks(request);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
      }

      @Test
      @DisplayName("유효한 커서와 limit이 들어오면 커서가 적용된 다음 페이지 목록을 리턴한다")
      void return_next_page_when_valid_cursor_provided() {
        // given
        Book mockBook = BookFixtureFactory.createBook4();

        BookCursor cursor = new BookCursor(
            mockBook.getTitle(),
            mockBook.getId(),
            mockBook.getCreatedAt()
        );

        String validCursor = CursorUtils.encodeCursor(cursor);

        BookGetListRequest request = BookGetListRequest.builder()
            .limit(2)
            .cursor(validCursor)
            .build();

        List<Book> mockBooks = List.of(BookFixtureFactory.createBook2(),
            BookFixtureFactory.createBook3());

        Slice<Book> mockSlice = new SliceImpl<>(mockBooks);

        given(bookRepository.getBooks(any(), any())).willReturn(mockSlice);

        // when
        PageResponse<BookDto> result = bookService.getBooks(request);

        // then
        assertThat(result.content()).isNotEmpty();

        verify(bookRepository, times(1)).getBooks(any(), any());
      }
    }

    @Nested
    @DisplayName("잘못 된 데이터가 들어왔을 경우")
    class TestGetBooks_InvalidData {

      @Test
      @DisplayName("limit이 음수로 들어왔을때 400에러 코드와 예외를 발생한다")
      void throw_exception_when_limit_is_negative() {

        // given
        BookGetListRequest request = BookGetListRequest.builder()
            .limit(-1) //음수 페이지 사이즈
            .build();

        // when & then
        assertThatThrownBy(() -> bookService.getBooks(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("잘못된 입력값입니다.");

        // 커서 디코딩 중 예외 발생 -> 리포지토리 접근 X
        verify(bookRepository, never()).getBooks(any(), any());
      }

      @Test
      @DisplayName("유효하지 않은 커서 문자열이 들어오면 Base64Exception을 던진다")
      void throw_exception_when_cursor_is_invalid() {
        // given
        String invalidCursor = "Wrong cursor format";

        BookGetListRequest request = BookGetListRequest.builder()
            .limit(50)
            .cursor(invalidCursor) //이상한 커서 삽입
            .build();

        // when & then
        assertThatThrownBy(() -> bookService.getBooks(request))
            .isInstanceOf(Base64Exception.class)
            .hasMessageContaining("잘못된 커서로 인해 디코딩에 실패했습니다");

        // 커서 디코딩 중 예외 발생 -> 리포지토리 접근 X
        verify(bookRepository, never()).getBooks(any(), any());
      }

    }

  }


  @Nested
  @DisplayName("도서 논리 삭제 서비스에서")
  class TestDeleteBook {


    UUID mockId = UUID.randomUUID();

    @Test
    @DisplayName("도서 테이블에서 해당 정보를 삭제하고 삭제 테이블로 옮긴다.")
    void deleteBook_success_and_move_to_deleted_table() {

      //given
      given(bookRepository.findById(any())).willReturn(
          Optional.of(BookFixtures.validBookToId(mockId)));

      DeletedBook deletedBook = BookFixtures.deletedBook(mockId);

      given(deletedBookRepository.save(any())).willReturn(deletedBook);

      //when
      bookService.delete(mockId);

      //then
      then(bookRepository).should().deleteById(mockId);
      then(deletedBookRepository).should().save(any(DeletedBook.class));

    }

    @Test
    @DisplayName("존재하지 않는 도서 ID로 삭제 시 예외가 발생한다")
    void deleteBook_fail_when_book_not_found() {
      // given
      given(bookRepository.findById(any())).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> bookService.delete(mockId))
          .isInstanceOf(BookNotFoundException.class);
    }
  }


  @Nested
  @DisplayName("도서 물리 삭제 서비스에서")
  class TestDeleteHardBook {

    @Test
    @DisplayName("유효한 도서 아이디가 들어올 경우 삭제한다")
    void successful_delete_book_hard() {

      UUID mockId = UUID.randomUUID();

      given(deletedBookRepository.findById(mockId)).willReturn(
          Optional.of(BookFixtures.deletedBook(mockId)));

      bookService.deleteHard(mockId);

      then(deletedBookRepository).should().findById(mockId);
      then(deletedBookRepository).should().deleteById(mockId);
      then(fileUploader).should().delete(any());
      then(fileUploader).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효한 도서 아이디가 아닐경우 예외를 발생시킨다")
    void fail_delete_book_hard_when_book_not_found() {

      given(deletedBookRepository.findById(any())).willReturn(Optional.empty());

      assertThatThrownBy(() -> bookService.deleteHard(UUID.randomUUID()))
          .isInstanceOf(BookNotFoundException.class);

      then(deletedBookRepository).should().findById(any());
      then(deletedBookRepository).shouldHaveNoMoreInteractions();
      then(fileUploader).shouldHaveNoMoreInteractions();
    }
  }


  private DataIntegrityViolationException createDataIntegrityViolation(String constraintName) {
    ConstraintViolationException cause = new ConstraintViolationException(
        "제약조건명 검증",
        new SQLException(),
        constraintName);
    return new DataIntegrityViolationException("데이터 무결성 예외 발생", cause);
  }
}