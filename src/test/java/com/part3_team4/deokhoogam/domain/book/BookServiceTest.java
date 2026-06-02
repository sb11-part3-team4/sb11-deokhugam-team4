package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.part3_team4.deokhoogam.domain.book.Factory.BookFixtureFactory;
import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.Base64Exception;
import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

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

        given(bookRepository.getBooks(any(),any())).willReturn(mockSlice);

        //when
        PageResponse<BookDto> result = bookService.getBooks(request);

        //then
        assertThat(result.getContent()).hasSize(mockBooks.size());
        assertThat(result.getContent().get(0).id()).isEqualTo(mockBooks.get(0).getId());

        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();

        verify(bookRepository, times(1)).getBooks(any(),any());
      }


      @Test
      @DisplayName("limit 크기가 하나면 다음 커서와 hasnext = true를 반환한다")
      void return_list_and_metadata_when_valid_data_with_limit_one() {

        //given
        BookGetListRequest request = BookGetListRequest.builder()
            .limit(1)
            .build();

        List<Book> mockBooks = List.of(BookFixtureFactory.createBook1());

        Pageable pageable = PageRequest.of(0, request.limit());
        Slice<Book> mockSlice = new SliceImpl<>(mockBooks,pageable,true);

        //when
        given(bookRepository.getBooks(any(),any())).willReturn(mockSlice);
        PageResponse<BookDto> result = bookService.getBooks(request);

        //then

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(mockBooks.get(0).getId());

        assertThat(result.isHasNext()).isTrue();

        assertThat(result.getNextCursor()).isNotNull();

      }

      @Test
      @DisplayName("조회된 데이터가 없으면 빈 리스트와 hasNext=false를 리턴한다")
      void return_empty_list_when_no_data() {
        // given
        BookGetListRequest request = BookGetListRequest.builder()
            .limit(50)
            .build();

        Slice<Book> mockSlice = new SliceImpl<>(List.of());

        given(bookRepository.getBooks(any(),any())).willReturn(mockSlice);

        // when
        PageResponse<BookDto> result = bookService.getBooks(request);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
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

        given(bookRepository.getBooks(any(),any())).willReturn(mockSlice);

        // when
        PageResponse<BookDto> result = bookService.getBooks(request);

        // then
        assertThat(result.getContent()).isNotEmpty();

        verify(bookRepository, times(1)).getBooks(any(),any());
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
        verify(bookRepository, never()).getBooks(any(),any());
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
        verify(bookRepository, never()).getBooks(any(),any());
      }

    }

  }

}
