package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.entity.BookDeletedEvent;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

  private final ReviewRepository reviewRepository;
  private final DeletedReviewRepository deletedReviewRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ReviewLikeRepository reviewLikeRepository;
  private final BookService bookService;
  private final PopularReviewRepository popularReviewRepository;

  //유저 삭제 이벤트 수신
  @EventListener
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    log.info("유저 삭제 이벤트 수신 - userId: {}, isHardDelete: {}", event.userId(), event.isHardDelete());

    List<Review> reviews = reviewRepository.findAllByUserId(event.userId());
    if (reviews.isEmpty()) {
      return;
    }

    //삭제되는 리뷰마다 하위 데이터 삭제를 위한 이벤트 발행
    reviews.forEach(review -> {
      eventPublisher.publishEvent(new ReviewDeletedEvent(review.getId(), event.isHardDelete()));
      reviewLikeRepository.deleteAllByReviewId(review.getId()); //좋아요 데이터 선행 삭제
      //삭제 시 인기 리뷰 선행 삭제
      popularReviewRepository.deleteAllByReviewId(review.getId());
    });

    if (!event.isHardDelete()) {
      //논리 삭제: 백업 테이블로 복사 후 원본 테이블에서 삭제
      List<DeletedReview> deletedReviews = reviews.stream()
          .map(DeletedReview::from)
          .toList();
      deletedReviewRepository.saveAll(deletedReviews);
      reviewRepository.deleteAll(reviews);
    }else {
      //물리 삭제: 원본 테이블에서 삭제(FK 에러 방지)
      reviewRepository.deleteAll(reviews);
    }

    //리뷰 삭제 후, 영향을 받은 도서들의 리뷰 수와 평점을 다시 계산해서 업데이트
    Set<UUID> affectedBookIds = reviews.stream()
        .map(Review::getBookId)
        .collect(Collectors.toSet());

    affectedBookIds.forEach(bookId -> {
      long reviewCount = reviewRepository.countByBookId(bookId);
      BigDecimal avgRating = reviewRepository.averageRatingByBookId(bookId);
      bookService.updateReviewData(bookId, Math.toIntExact(reviewCount), avgRating);
      log.info("도서 통계 업데이트 완료 - bookId: {}, count: {}, rating: {}", bookId, reviewCount, avgRating);
    });
  }

  //도서 삭제 이벤트 수신
  @EventListener
  public void handleBookDeletedEvent(BookDeletedEvent event) {
    log.info("도서 삭제 이벤트 수신 - bookId: {}, isHardDelete: {}", event.bookId(), event.isHardDelete());

    List<Review> reviews = reviewRepository.findAllByBookId(event.bookId());
    if (reviews.isEmpty()) {
      return;
    }

    //삭제되는 리뷰마다 하위 데이터 삭제를 위한 이벤트 발행
    reviews.forEach(review -> {
      eventPublisher.publishEvent(new ReviewDeletedEvent(review.getId(), event.isHardDelete()));
      reviewLikeRepository.deleteAllByReviewId(review.getId()); // 좋아요 데이터 선행 삭제 (FK 에러 방지)
      //삭제 시 인기 리뷰 선행 삭제
      popularReviewRepository.deleteAllByReviewId(review.getId());
    });

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
