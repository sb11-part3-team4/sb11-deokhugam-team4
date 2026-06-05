package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.instructure.naver.NaverApiService;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Book 통합 테스트")
class BookIntegrationTest {

  @Autowired
  private BookService bookService;

  @Autowired
  private NaverApiService naverApiService;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private EntityManager em;

  @MockitoBean
  private FileUploader fileUploader;

  private static final String BOOK_THUMBNAIL_DIR = "books";

  @Test
  @DisplayName("도서를 생성하면 DB에 저장되고 S3 URL이 함께 반영된다")
  void createBook_success() {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();

    MockMultipartFile file = new MockMultipartFile(
        "file", "image.png", "image/png", "content".getBytes()
    );

    String uploadedUrl = "https://s3.com/books/image.png";

    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR)))
        .willReturn(uploadedUrl);

    // when
    BookDto result = bookService.create(request, file);

    em.flush();
    em.clear();

    // then
    assertThat(result.id()).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo(uploadedUrl);

    Book saved = bookRepository.findById(result.id())
        .orElseThrow();

    assertThat(saved.getTitle()).isEqualTo(request.title());
    assertThat(saved.getIsbn()).isEqualTo(request.isbn());
    assertThat(saved.getThumbnailUrl()).isEqualTo(uploadedUrl);
  }

  @Test
  @DisplayName("도서를 수정하면 DB에 dirty checking으로 반영된다")
  void updateBook_success() {
    // given
    Book book = bookRepository.save(BookFixtures.validBook("9781234567890"));

    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    MockMultipartFile newFile = new MockMultipartFile(
        "file", "new.png", "image/png", "new".getBytes()
    );

    String newUrl = "https://s3.com/books/new.png";

    given(fileUploader.upload(any(MultipartFile.class), eq(BOOK_THUMBNAIL_DIR)))
        .willReturn(newUrl);

    // when
    BookDto result = bookService.update(book.getId(), request, newFile);

    em.flush();
    em.clear();

    // then
    assertThat(result.title()).isEqualTo(request.title());
    assertThat(result.thumbnailUrl()).isEqualTo(newUrl);

    Book updated = bookRepository.findById(book.getId())
        .orElseThrow();

    assertThat(updated.getTitle()).isEqualTo(request.title());
    assertThat(updated.getAuthor()).isEqualTo(request.author());
    assertThat(updated.getThumbnailUrl()).isEqualTo(newUrl);
  }

  @Test
  @DisplayName("중복 ISBN이면 DB 제약 조건으로 인해 예외가 발생한다")
  void createBook_duplicate_isbn() {
    // given
    String isbn = "9781234567890";

    bookRepository.save(BookFixtures.validBook(isbn));

    em.flush();
    em.clear();

    BookCreateRequest request = BookFixtures.validBookCreateRequest()
        .toBuilder()
        .isbn(isbn)
        .build();

    // when & then
    assertThatThrownBy(() ->
        bookService.create(request, null)
    )
        .isInstanceOf(IsbnAlreadyExistsException.class);
  }
}