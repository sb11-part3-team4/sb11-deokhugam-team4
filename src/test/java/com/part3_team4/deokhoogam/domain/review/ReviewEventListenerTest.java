package com.part3_team4.deokhoogam.domain.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.part3_team4.deokhoogam.domain.book.entity.BookDeletedEvent;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.review.service.ReviewEventListener;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReviewEventListenerTest {

  @InjectMocks
  private ReviewEventListener reviewEventListener;

  @Mock private ReviewRepository reviewRepository;
  @Mock private DeletedReviewRepository deletedReviewRepository;
  @Mock private BookService bookService;
  @Mock private ReviewLikeRepository reviewLikeRepository;
  @Mock private PopularReviewRepository popularReviewRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  // 유저 삭제 이벤트 테스트

  @Test
  @DisplayName("유저 삭제 시 작성한 리뷰가 없으면 로직을 종료")
  void handleUserDeletedEvent_EmptyReviews() {
    UUID userId = UUID.randomUUID();
    when(reviewRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

    reviewEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, false));

    verify(eventPublisher, never()).publishEvent(any());
    verify(deletedReviewRepository, never()).saveAll(any());
    verify(reviewRepository, never()).deleteAll(any());
    verify(bookService, never()).updateReviewData(any(), anyInt(), any());
  }

  @Test
  @DisplayName("유저 논리 삭제 시 리뷰를 백업하고 연관 데이터를 삭제하며 도서 통계를 업데이트")
  void handleUserDeletedEvent_SoftDelete() {
    UUID userId = UUID.randomUUID();
    Review review = mock(Review.class);
    when(review.getId()).thenReturn(UUID.randomUUID());
    when(review.getBookId()).thenReturn(UUID.randomUUID());
    when(reviewRepository.findAllByUserId(userId)).thenReturn(List.of(review));
    when(reviewRepository.countByBookId(any())).thenReturn(1L);
    when(reviewRepository.averageRatingByBookId(any())).thenReturn(BigDecimal.valueOf(4.5));

    reviewEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, false));

    verify(eventPublisher, times(1)).publishEvent(any(ReviewDeletedEvent.class));
    verify(reviewLikeRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(popularReviewRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(deletedReviewRepository, times(1)).saveAll(any());
    verify(reviewRepository, times(1)).deleteAll(any());
    verify(bookService, times(1)).updateReviewData(any(), eq(1), eq(BigDecimal.valueOf(4.5)));
  }

  @Test
  @DisplayName("유저 물리 삭제 시 리뷰 백업 없이 원본과 연관 데이터를 삭제하고 도서 통계를 업데이트")
  void handleUserDeletedEvent_HardDelete() {
    UUID userId = UUID.randomUUID();
    Review review = mock(Review.class);
    when(review.getId()).thenReturn(UUID.randomUUID());
    when(review.getBookId()).thenReturn(UUID.randomUUID());
    when(reviewRepository.findAllByUserId(userId)).thenReturn(List.of(review));
    when(reviewRepository.countByBookId(any())).thenReturn(0L);
    when(reviewRepository.averageRatingByBookId(any())).thenReturn(BigDecimal.ZERO);

    reviewEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true)); // 물리 삭제

    verify(eventPublisher, times(1)).publishEvent(any(ReviewDeletedEvent.class));
    verify(deletedReviewRepository, never()).saveAll(any()); // 하드 삭제 시 백업 안 함
    verify(reviewRepository, times(1)).deleteAll(any());
    verify(bookService, times(1)).updateReviewData(any(), eq(0), eq(BigDecimal.ZERO));
  }

  // 도서 삭제 이벤트 테스트

  @Test
  @DisplayName("도서 삭제 시 작성된 리뷰가 없으면 로직을 종료")
  void handleBookDeletedEvent_EmptyReviews() {
    UUID bookId = UUID.randomUUID();
    when(reviewRepository.findAllByBookId(bookId)).thenReturn(Collections.emptyList());

    reviewEventListener.handleBookDeletedEvent(new BookDeletedEvent(bookId, false));

    verify(eventPublisher, never()).publishEvent(any());
    verify(deletedReviewRepository, never()).saveAll(any());
    verify(reviewRepository, never()).deleteAll(any());
  }

  @Test
  @DisplayName("도서 논리 삭제 시 리뷰를 백업하고 원본과 연관 데이터를 삭제")
  void handleBookDeletedEvent_SoftDelete() {
    UUID bookId = UUID.randomUUID();
    Review review = mock(Review.class);
    when(review.getId()).thenReturn(UUID.randomUUID());
    when(reviewRepository.findAllByBookId(bookId)).thenReturn(List.of(review));

    reviewEventListener.handleBookDeletedEvent(new BookDeletedEvent(bookId, false));

    verify(eventPublisher, times(1)).publishEvent(any(ReviewDeletedEvent.class));
    verify(reviewLikeRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(popularReviewRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(deletedReviewRepository, times(1)).saveAll(any());
    verify(reviewRepository, times(1)).deleteAll(any());
  }

  @Test
  @DisplayName("도서 물리 삭제 시 리뷰 백업 없이 원본과 연관 데이터를 삭제")
  void handleBookDeletedEvent_HardDelete() {
    UUID bookId = UUID.randomUUID();
    Review review = mock(Review.class);
    when(review.getId()).thenReturn(UUID.randomUUID());
    when(reviewRepository.findAllByBookId(bookId)).thenReturn(List.of(review));

    reviewEventListener.handleBookDeletedEvent(new BookDeletedEvent(bookId, true)); // 물리 삭제

    verify(eventPublisher, times(1)).publishEvent(any(ReviewDeletedEvent.class));
    verify(reviewLikeRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(popularReviewRepository, times(1)).deleteAllByReviewId(any(UUID.class));
    verify(deletedReviewRepository, never()).saveAll(any()); // 하드 삭제 시 백업 안 함
    verify(reviewRepository, times(1)).deleteAll(any());
  }
}