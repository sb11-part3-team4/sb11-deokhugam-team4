package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.entity.BookDeletedEvent;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {
  private final ReviewRepository reviewRepository;
  private final DeletedReviewRepository deletedReviewRepository;

  //유저 삭제 이벤트 수신
  @EventListener
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    log.info("유저 삭제 이벤트 수신 - userId: {}, isHardDelete: {}", event.userId(), event.isHardDelete());

    List<Review> reviews = reviewRepository.findAllByUserId(event.userId());
    if (reviews.isEmpty()) {
      return;
    }

    if (!event.isHardDelete()) {
      //논리 삭제: 백업 테이블로 복사 후 원본 테이블에서 삭제
      List<DeletedReview> deletedReviews = reviews.stream()
          .map(DeletedReview::from)
          .toList();
      deletedReviewRepository.saveAll(deletedReviews);
      reviewRepository.deleteAll(reviews);
    } else {
      //물리 삭제: 원본 테이블에서 삭제(FK 에러 방지)
      reviewRepository.deleteAll(reviews);
    }
  }

  //도서 삭제 이벤트 수신
  @EventListener
  public void handleBookDeletedEvent(BookDeletedEvent event) {
    log.info("도서 삭제 이벤트 수신 - userId: {}, isHardDelete: {}", event.bookId(), event.isHardDelete());

    List<Review> reviews = reviewRepository.findAllByBookId(event.bookId());
    if (reviews.isEmpty()) {
      return;
    }

    if (!event.isHardDelete()) {
      //논리 삭제: 백업 테이블로 복사 후 원본 테이블에서 삭제
      List<DeletedReview> deletedReviews = reviews.stream()
          .map(DeletedReview::from)
          .toList();
      deletedReviewRepository.saveAll(deletedReviews);
      reviewRepository.deleteAll(reviews);
    } else {
      //물리 삭제: 원본 테이블에서 삭제(FK 에러 방지)
      reviewRepository.deleteAll(reviews);
    }
  }
}
