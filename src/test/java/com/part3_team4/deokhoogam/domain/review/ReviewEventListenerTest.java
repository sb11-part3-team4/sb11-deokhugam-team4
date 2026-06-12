package com.part3_team4.deokhoogam.domain.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.*;
import com.part3_team4.deokhoogam.domain.review.service.ReviewEventListener;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
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
  @Mock private ApplicationEventPublisher eventPublisher; // NullPoint 방지

  @Test
  @DisplayName("유저 삭제 이벤트(논리 삭제) 발생 시 리뷰 백업 및 연관 데이터 삭제 확인")
  void handleUserDeletedEvent_SoftDelete() {
    UUID userId = UUID.randomUUID();
    Review review = mock(Review.class);
    when(review.getId()).thenReturn(UUID.randomUUID());
    when(review.getBookId()).thenReturn(UUID.randomUUID()); // 통계 업데이트를 위해 bookId 필요
    when(reviewRepository.findAllByUserId(userId)).thenReturn(List.of(review));

    // when
    reviewEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, false));

    // then
    verify(deletedReviewRepository).saveAll(any());
    verify(reviewRepository).deleteAll(any());
    verify(bookService).updateReviewData(any(), anyInt(), any());
  }
}