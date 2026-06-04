package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import com.part3_team4.deokhoogam.domain.book.service.NaverApiService;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.fixture.NaverBookFixture;
import java.sql.SQLException;
import java.time.LocalDate;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

  @InjectMocks
  private BookServiceImpl bookService;

  @Mock
  private NaverApiService naverApiService;

  @Mock
  private BookRepository bookRepository;

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
    BookDto result = bookService.create(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(mockId);
    assertThat(result.isbn()).isEqualTo(request.isbn());
    assertThat(result.title()).isEqualTo(request.title());

    // ArgumentCaptor로 실제 save에 전달된 엔티티 필드 검증 수행
    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    then(bookRepository).should().save(bookCaptor.capture());

    Book actualSavedBook = bookCaptor.getValue();
    assertThat(actualSavedBook.getIsbn()).isEqualTo(request.isbn());
    assertThat(actualSavedBook.getTitle()).isEqualTo(request.title());
    assertThat(actualSavedBook.getAuthor()).isEqualTo(request.author());
  }

  @Test
  @DisplayName("이미 존재하는 ISBN으로 도서를 등록하면 예외가 발생한다")
  void registerBook_WithAlreadyExistIsbn_ThrowsException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(IsbnAlreadyExistsException.class);

    then(bookRepository).should().existsByIsbn(request.isbn());
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("ISBN 제약 위반시 IsbnAlreadyExistsException으로 변환")
  void create_uniqueViolationOnIsbn_throwsBusinessException() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    ConstraintViolationException cause = new ConstraintViolationException(
        "제약조건명 검증",
        new SQLException(),
        "uk_book_isbn"
    );
    DataIntegrityViolationException exception = new DataIntegrityViolationException("예외 발생", cause);

    given(bookRepository.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThrows(IsbnAlreadyExistsException.class,
        () -> bookService.create(request));
  }

  @Test
  @DisplayName("ISBN 외 제약 위반 발생 시 원본 예외 그대로 전파한다")
  void create_dataIntegrityViolationOnOtherConstraint_propagates() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    ConstraintViolationException cause = new ConstraintViolationException(
        "제약조건명 검증",
        new SQLException(),
        "other_constraint"
    );
    DataIntegrityViolationException exception = new DataIntegrityViolationException("예외 발생", cause);

    given(bookRepository.save(any(Book.class))).willThrow(exception);

    assertThrows(DataIntegrityViolationException.class,
        () -> bookService.create(request));
  }

  @Test
  @DisplayName("원인이 ConstraintViolationException이 아닌 DataIntegrityViolationException 발생 시 원본 예외를 전파한다")
  void create_dataIntegrityViolation_withDifferentCause_propagates() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);

    SQLException differentCause = new SQLException("일반적인 SQL 문법 에러 등");
    DataIntegrityViolationException exception = new DataIntegrityViolationException("예외 발생",
        differentCause);

    given(bookRepository.save(any(Book.class))).willThrow(exception);

    // when & then
    assertThrows(DataIntegrityViolationException.class,
        () -> bookService.create(request));
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

    // @Transactional + 더티 체킹으로 save() 명시적 호출 없이 자동 반영
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
      given(naverApiService.getBookInfoByIsbn(validIsbn)).willReturn(NaverBookFixture.createValidNaverBookDto(validIsbn));

      // when
      NaverBookDto result = bookService.getByIsbn(validIsbn);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("모비 딕");
      assertThat(result.getPublisher()).isEqualTo("작가정신");
    }

    @Test
    @DisplayName("ISBN 형식에 입력시 400 과 예외를 발생시킨다")
    void return_400_when_invalid_isbn(){

      String invalidIsbn = "978895727254";

      // when & then
      assertThatThrownBy(() -> bookService.getByIsbn(invalidIsbn)).isInstanceOf(InvalidIsbnException.class);
      then(naverApiService).shouldHaveNoMoreInteractions();

    }

    @Test
    @DisplayName("네이버 API에 없는 책이라면 404 예외를 발생시킨다")
    void return_404_when_book_not_found_by_isbn(){


      String notFoundIsbn = "9788957272542";
      given(naverApiService.getBookInfoByIsbn(notFoundIsbn)).willReturn(null);

      assertThatThrownBy(() -> bookService.getByIsbn(notFoundIsbn)).isInstanceOf(BookNotFoundException.class);
      then(naverApiService).should().getBookInfoByIsbn(notFoundIsbn);
      then(naverApiService).shouldHaveNoMoreInteractions();

    }





  }















  private BookUpdateRequest createValidBookUpdateRequest() {
    return BookUpdateRequest.builder()
        .title("클린 아키텍처")
        .author("로버트 C. 마틴")
        .description("소프트웨어 구조와 설계의 원칙")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 29))
        .build();
  }

}