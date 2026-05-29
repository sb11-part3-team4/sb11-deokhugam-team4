package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

  @InjectMocks
  private BookServiceImpl bookService;

  @Mock
  private BookRepository bookRepository;

  @Test
  @DisplayName("새로운 도서를 성공적으로 등록한다")
  void createBook_Success() {
    // given
    BookCreateRequest request = createValidBookRequest();
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
    org.mockito.ArgumentCaptor<Book> bookCaptor = org.mockito.ArgumentCaptor.forClass(Book.class);
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
    BookCreateRequest request = createValidBookRequest();

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(IsbnAlreadyExistsException.class);

    then(bookRepository).should().existsByIsbn(request.isbn());
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("도서 등록 중 동시성 이슈로 DB Unique 제약조건이 깨지면 예외가 발생한다")
  void createBook_concurrentSave_throwsIsbnAlreadyExistsException() {
    // given
    BookCreateRequest request = createValidBookRequest();

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(false);
    given(bookRepository.save(any(Book.class)))
        .willThrow(new DataIntegrityViolationException("ISBN 동시 입력"));

    // when & then
    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(IsbnAlreadyExistsException.class);

    then(bookRepository).should().existsByIsbn(request.isbn());

    org.mockito.ArgumentCaptor<Book> bookCaptor = org.mockito.ArgumentCaptor.forClass(Book.class);
    then(bookRepository).should().save(bookCaptor.capture());

    Book actualSavedBook = bookCaptor.getValue();
    assertThat(actualSavedBook.getIsbn()).isEqualTo(request.isbn());
  }

  // 픽스처 메서드
  private BookCreateRequest createValidBookRequest() {
    return BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();
  }


  @Nested
  @DisplayName("getBook() 메서드")
  class testGetBookDetails {

    UUID mockId = UUID.randomUUID();


    @Test
    @DisplayName("유효한 id가 들어왔을때 책 정보를 반환한다.")
    void returnDetails(){

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
    public void returnDetailsInvalidId(){

      //given
      given(bookRepository.findById(mockId)).willReturn(Optional.empty());
      //then & when
      assertThatThrownBy(()-> bookService.getDetails(mockId)).isInstanceOf(BookNotFoundException.class);

    }




    private Optional<Book> book (){

      return Optional.of(
          Book.builder()
              .title("모비 딕")
              .author("허먼 멜빌")
              .description("『모비 딕』 완역본")
              .publisher("작가정신")
              .publishedDate(LocalDate.of(2024, 4, 9))
              .thumbnailUrl("temp/url")
              .isbn("9791160263404")
              .build()
      );


    }


  }





















}