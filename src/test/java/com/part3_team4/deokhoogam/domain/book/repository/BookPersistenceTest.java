package com.part3_team4.deokhoogam.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookPersistence 단위 테스트")
class BookPersistenceTest {

  @InjectMocks
  private BookPersistence bookPersistence;

  @Mock
  private BookRepository bookRepository;

  @Test
  @DisplayName("도서 저장 요청 시 리포지토리에 저장을 위임하고 결과를 반환한다")
  void save_Success() {
    // given
    Book book = BookFixtures.validBook("1234567890123");
    given(bookRepository.save(any(Book.class))).willReturn(book);

    // when
    Book result = bookPersistence.save(book);

    // then
    assertThat(result).isEqualTo(book);
    then(bookRepository).should().save(book);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("새로운 썸네일이 전달되면 도서 정보와 썸네일을 함께 수정한다")
  void update_WithNewThumbnail_UpdatesBookAndThumbnail() {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    String oldThumbnailUrl = "https://s3.com/old-image.png";
    String oldOriginalFilename = "old-image.png";

    String newThumbnailUrl = "https://s3.com/new-image.png";
    String newOriginalFilename = "new-image.png";

    Book existingBook = BookFixtures.validBook("1234567890123");

    ReflectionTestUtils.setField(existingBook, "thumbnailUrl", oldThumbnailUrl);
    ReflectionTestUtils.setField(existingBook, "originalFilename", oldOriginalFilename);

    given(bookRepository.findById(targetId))
        .willReturn(Optional.of(existingBook));

    // when
    Book result = bookPersistence.update(
        targetId,
        request,
        newThumbnailUrl,
        newOriginalFilename);

    // then
    assertThat(result.getTitle()).isEqualTo(request.title());
    assertThat(result.getAuthor()).isEqualTo(request.author());
    assertThat(result.getDescription()).isEqualTo(request.description());
    assertThat(result.getPublisher()).isEqualTo(request.publisher());
    assertThat(result.getPublishedDate()).isEqualTo(request.publishedDate());

    assertThat(result.getThumbnailUrl()).isEqualTo(newThumbnailUrl);
    assertThat(result.getOriginalFilename()).isEqualTo(newOriginalFilename);

    then(bookRepository).should().findById(targetId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("새로운 썸네일이 없으면 기존 썸네일을 유지하고 도서 정보만 수정한다")
  void update_WithoutThumbnail_KeepsExistingThumbnail() {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    String oldThumbnailUrl = "https://s3.com/old-image.png";
    String oldOriginalFilename = "old-image.png";

    Book existingBook = BookFixtures.validBook("1234567890123");

    ReflectionTestUtils.setField(existingBook, "thumbnailUrl", oldThumbnailUrl);
    ReflectionTestUtils.setField(existingBook, "originalFilename", oldOriginalFilename);

    given(bookRepository.findById(targetId))
        .willReturn(Optional.of(existingBook));

    // when
    Book result = bookPersistence.update(
        targetId,
        request,
        null,
        null);

    // then
    assertThat(result.getTitle()).isEqualTo(request.title());
    assertThat(result.getAuthor()).isEqualTo(request.author());
    assertThat(result.getDescription()).isEqualTo(request.description());
    assertThat(result.getPublisher()).isEqualTo(request.publisher());
    assertThat(result.getPublishedDate()).isEqualTo(request.publishedDate());

    assertThat(result.getThumbnailUrl()).isEqualTo(oldThumbnailUrl);
    assertThat(result.getOriginalFilename()).isEqualTo(oldOriginalFilename);

    then(bookRepository).should().findById(targetId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 수정 요청 시 BookNotFoundException을 던진다")
  void update_WithNonExistentId_ThrowsException() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

    given(bookRepository.findById(nonExistentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() ->
        bookPersistence.update(nonExistentId, request, null, null))
        .isInstanceOf(BookNotFoundException.class);

    then(bookRepository).should().findById(nonExistentId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("리뷰 통계가 정상적으로 업데이트된다")
  void updateReviewData_Success() {
    // given
    UUID targetId = UUID.randomUUID();
    Book existingBook = BookFixtures.validBook("1234567890123");

    int newReviewCount = 5;
    BigDecimal newRating = new BigDecimal("4.50");

    given(bookRepository.findById(targetId))
        .willReturn(Optional.of(existingBook));

    // when
    bookPersistence.updateReviewData(targetId, newReviewCount, newRating);

    // then
    assertThat(existingBook.getReviewCount()).isEqualTo(newReviewCount);
    assertThat(existingBook.getRating()).isEqualTo(newRating);

    then(bookRepository).should().findById(targetId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 통계 업데이트 요청 시 BookNotFoundException을 던진다")
  void updateReviewData_WithNonExistentId_ThrowsException() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    given(bookRepository.findById(nonExistentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() ->
        bookPersistence.updateReviewData(nonExistentId, 5, new BigDecimal("4.50")))
        .isInstanceOf(BookNotFoundException.class);

    then(bookRepository).should().findById(nonExistentId);
    then(bookRepository).shouldHaveNoMoreInteractions();
  }
}