package com.part3_team4.deokhoogam.domain.review;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.InvalidReviewException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotOwnerException;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.review.service.ReviewServiceImpl;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @InjectMocks
    ReviewServiceImpl reviewService;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    BookRepository bookRepository;
    @Mock
    ReviewLikeRepository reviewLikeRepository;
    @Mock
    DeletedReviewRepository deletedReviewRepository;

    @Test
    @DisplayName("정상적인 요청으로 리뷰를 등록하면 ReviewResponse를 반환한다")
    void createReview_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(reviewRepository.existsByUserIdAndBookId(userId, bookId)).willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(i -> i.getArgument(0));

        ReviewResponse response = reviewService.createReview(userId, request);

        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("좋은 책이에요");
    }

    @Test
    @DisplayName("동일 도서에 이미 리뷰가 존재하면 ReviewAlreadyExistsException을 던진다")
    void createReview_duplicateReview_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(reviewRepository.existsByUserIdAndBookId(userId, bookId)).willReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    @DisplayName("존재하지 않는 bookId로 등록하면 BookNotFoundException을 던진다")
    void createReview_bookNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(BookNotFoundException.class);
    }


    @Test
    @DisplayName("reviewId로 리뷰를 조회하면 ReviewResponse를 반환한다")
    void getReview_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "좋은 책이에요");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(UUID.class), eq(userId))).willReturn(false);

        ReviewResponse response = reviewService.getReview(review.getId(), userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.likedByMe()).isFalse();

    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 조회하면 ReviewNotFoundException을 던진다")
    void getReview_reviewNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(reviewId, userId)).isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("타인의 reviewId를 수정하려하면 ReviewNotOwnerException을 던진다")
    void updateReview_reviewNotOwner_throwsException() {
        UUID ownerUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");
        ReviewUpdateRequest request = new ReviewUpdateRequest(4, "수정된 내용");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, anotherUserId, request)).isInstanceOf(ReviewNotOwnerException.class);

    }

    @Test
    @DisplayName("reviewId로 리뷰를 수정하면 ReviewResponse를 반환한다")
    void updateReview_success() {
        UUID ownerUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "좋은 책이에요");
        ReviewUpdateRequest request = new ReviewUpdateRequest(5, "수정된 내용");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));

        ReviewResponse response = reviewService.updateReview(review.getId(), ownerUserId, request);

        assertThat(response.userId()).isEqualTo(ownerUserId);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 수정하면 ReviewNotFoundException을 던진다")
    void updateReview_reviewNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest(4,"수정된 내용");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, userId, request))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("rating이 범위를 벗어나면 InvalidReviewException을 던진다")
    void updateReview_invalidRating_throwsException() {
        UUID ownerUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");
        ReviewUpdateRequest request = new ReviewUpdateRequest(6, "수정된 내용");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, ownerUserId, request))
                .isInstanceOf(InvalidReviewException.class);
    }

    @Test
    @DisplayName("논리 삭제 성공 시 DeletedReview가 저장된다")
    void deleteReview_success_deletedReviewSaved() {
        UUID ownerUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));

        reviewService.deleteReview(review.getId(), ownerUserId);

        then(deletedReviewRepository).should().save(any(DeletedReview.class));
    }

    @Test
    @DisplayName("논리 삭제 성공 시 Review 원본이 삭제된다")
    void deleteReview_success_reviewDeleted() {
        UUID ownerUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));

        reviewService.deleteReview(review.getId(), ownerUserId);

        then(reviewRepository).should().delete(review);

    }

    @Test
    @DisplayName("타인의 reviewId를 삭제하려하면 ReviewNotOwnerException을 던진다")
    void deleteReview_reviewNotOwner_throwsException() {
        UUID ownerUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");


        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, anotherUserId)).isInstanceOf(ReviewNotOwnerException.class);

    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 삭제를 시도하면 ReviewNotFoundException을 던진다")
    void deleteReview_reviewNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, userId)).isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("리뷰를 삭제하면 ReviewLike도 함께 삭제된다")
    void deleteReview_success_deleteReviewLike() {
        UUID ownerUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(ownerUserId, bookId, 4, "내용");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));

        reviewService.deleteReview(review.getId(), ownerUserId);

        then(reviewLikeRepository).should().deleteAllByReviewId(review.getId());
    }

    @Test
    @DisplayName("좋아요가 없는 상태에서 토글하면 liked=true, likeCount가 1 증가한다")
    void toggleLike_like_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "내용");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(UUID.class),eq(userId))).willReturn(false);

        ReviewLikeResponse response = reviewService.toggleLike(review.getId(),userId);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 좋아요 상태에서 토글하면 liked=false, likeCount가 1 감소한다")
    void toggleLike_unlike_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "내용");
        review.incrementLikeCount();

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(UUID.class),eq(userId))).willReturn(true);

        ReviewLikeResponse response = reviewService.toggleLike(review.getId(), userId);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 좋아요를 누르면 ReviewNotFoundException을 던진다")
    void toggleLike_reviewNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.toggleLike(reviewId, userId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("조건 없이 조회하면 전체 리뷰 목록을 반환한다")
    void getReviews_success() {
        UUID requestUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), bookId, 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), bookId, 3, "그냥 그래요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.hasNext()).isFalse();

    }

    @Test
    @DisplayName("유저명으로 리뷰를 검색하면 해당 유저의 리뷰만 반환한다")
    void getReviews_userID() {
        UUID targetUserId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(targetUserId, bookId, 4, "좋은 책이에요");
        Review review2 = Review.create(targetUserId, bookId, 2, "별로에요");

        ReviewListRequest request = new ReviewListRequest(
                targetUserId, null, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).allMatch(r -> r.userId().equals(targetUserId));
    }

    @Test
    @DisplayName("도서명으로 리뷰를 검색하면 해당 도서의 리뷰만 반환한다")
    void getReviews_bookId() {
        UUID targetBookId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), targetBookId, 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), targetBookId, 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, targetBookId, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).allMatch(r -> r.bookId().equals(targetBookId));
    }

    @Test
    @DisplayName("keyword으로 리뷰를 검색하면 해당 keyword가 포함된 리뷰만 반환한다")
    void getReviews_keyWord() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, "좋은", "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
    }

    @Test
    @DisplayName("createdAt 내림차순 정렬로 조회하면 최신 리뷰가 먼저 반환된다")
    void getReviews_createdAt() {
        UUID requestUserId = UUID.randomUUID();


        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content().get(0).id()).isEqualTo(review1.getId());
        assertThat(response.content().get(1).id()).isEqualTo(review2.getId());

    }

    @Test
    @DisplayName("rating 내림차순 정렬로 조회하면 높은 평점의 리뷰가 먼저 반환된다")
    void getReviews_rating() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "rating","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(false);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content().get(0).id()).isEqualTo(review1.getId());
        assertThat(response.content().get(1).id()).isEqualTo(review2.getId());
    }

    @Test
    @DisplayName("요청자가 좋아요를 누른 리뷰는 likedByMe가 true로 반환된다")
    void getReviews_likedByMe() {
        UUID requestUserId = UUID.randomUUID();


        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.existsByReviewIdAndUserId(any(), eq(requestUserId)))
                .willReturn(true);

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).allMatch(r -> r.likedByMe());
    }

    @Test
    @DisplayName("검색 결과가 0건 이면 빈 배열과 hasNext=false를 반환한다")
    void getReviews_emptyResult() {
        UUID requestUserId = UUID.randomUUID();

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt","DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(ReviewListRequest.class)))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }

}
