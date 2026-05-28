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
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookServiceImpl;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    BookCreateRequest request = BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();

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

    then(bookRepository).should().save(any(Book.class));
  }

  @Test
  @DisplayName("이미 존재하는 ISBN으로 도서를 등록하면 예외가 발생한다")
  void registerBook_WithAlreadyExistIsbn_ThrowsException() {
    // given
    BookCreateRequest request = BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();

    given(bookRepository.existsByIsbn(request.isbn())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(IsbnAlreadyExistsException.class);

    then(bookRepository).should().existsByIsbn(request.isbn());
    then(bookRepository).shouldHaveNoMoreInteractions();
  }
}