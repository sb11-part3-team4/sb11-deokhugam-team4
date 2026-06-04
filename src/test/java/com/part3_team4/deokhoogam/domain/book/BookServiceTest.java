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
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.DeletedBookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import java.sql.SQLException;
import java.time.Instant;
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
  private BookRepository bookRepository;

  @Mock
  private DeletedBookRepository deletedBookRepository;

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
  @DisplayName("도서 논리 삭제 서비스에서")
  class TestDeleteBook {


    UUID mockId = UUID.randomUUID();

    @Test
    @DisplayName("도서 테이블에서 해당 정보를 삭제하고 삭제 테이블로 옮긴다.")
    void deleteBook_success_and_move_to_deleted_table() {

      //given
      given(bookRepository.findById(any())).willReturn(Optional.of(createBook(mockId)));

      DeletedBook deletedBook = DeletedBook.from(createBook(mockId));
      ReflectionTestUtils.setField(deletedBook, "deletedAt", Instant.now());

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


    private Book createBook(UUID mockId) {

      Instant createdAt = Instant.now().minusSeconds(100);
      Instant updatedAt = Instant.now().minusSeconds(50);

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
      ReflectionTestUtils.setField(book, "createdAt", createdAt);
      ReflectionTestUtils.setField(book, "updatedAt", updatedAt);

      return book;

    }

  }

  @Nested
  @DisplayName("도서 물리 삭제 서비스에서")
  class TestDeleteHardBook {

    @Test
    @DisplayName("유효한 도서 아이디가 들어올 경우 삭제한다")
    void successful_delete_book_hard() {

      given(deletedBookRepository.existsById(any())).willReturn(true);

      bookService.deleteHard(UUID.randomUUID());

      then(deletedBookRepository).should().existsById(any());
      then(deletedBookRepository).should().deleteById(any());

    }

    @Test
    @DisplayName("유효한 도서 아이디가 아닐경우 예외를 발생시킨다")
    void fail_delete_book_hard_when_book_not_found() {

      given(deletedBookRepository.existsById(any())).willReturn(false);

      assertThatThrownBy(() -> bookService.deleteHard(UUID.randomUUID()))
          .isInstanceOf(BookNotFoundException.class);

      then(deletedBookRepository).should().existsById(any());
      then(deletedBookRepository).shouldHaveNoMoreInteractions();
    }


  }




}