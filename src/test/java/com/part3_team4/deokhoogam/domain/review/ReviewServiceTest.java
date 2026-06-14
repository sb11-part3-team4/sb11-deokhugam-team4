package com.part3_team4.deokhoogam.domain.review;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.InvalidReviewException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotOwnerException;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.review.service.ReviewServiceImpl;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;


import java.math.BigDecimal;

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
    @Mock
    PopularReviewRepository popularReviewRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    BookService bookService;
    @Mock
    NotificationService notificationService;

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

        User user = new User("test@test.com", "테스트유저", "password");

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(reviewRepository.existsByUserIdAndBookId(userId, bookId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(reviewRepository.save(any(Review.class))).willAnswer(i -> i.getArgument(0));
        given(reviewRepository.countByBookId(bookId)).willReturn(1L);
        given(reviewRepository.averageRatingByBookId(bookId)).willReturn(new BigDecimal("4.0"));

        ReviewResponse response = reviewService.createReview(userId, request);

        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("좋은 책이에요");

        then(bookService).should().updateReviewData(bookId, 1, new BigDecimal("4.00"));
    }

    @Test
    @DisplayName("동일 도서에 이미 리뷰가 존재하면 ReviewAlreadyExistsException을 던진다")
    void createReview_duplicateReview_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

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

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        User user = new User("test@test.com", "테스트유저", "password");

        given(reviewRepository.findByIdWithLiked(any(UUID.class), eq(userId)))
                .willReturn(Optional.of(new ReviewWithLiked(review, false)));
        given(bookRepository.findById(any(UUID.class))).willReturn(Optional.of(book));
        given(userRepository.findById(review.getUserId())).willReturn(Optional.of(user));

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

        given(reviewRepository.findByIdWithLiked(eq(reviewId), any(UUID.class)))
                .willReturn(Optional.empty());

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

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        User user = new User("test@test.com", "테스트유저", "password");

        given(reviewRepository.findById(any(UUID.class))).willReturn(Optional.of(review));
        given(bookRepository.findById(any(UUID.class))).willReturn(Optional.of(book));
        given(userRepository.findById(ownerUserId)).willReturn(Optional.of(user));

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

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        User user = new User("test@test.com", "테스트유저", "password");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(bookRepository.findById(any(UUID.class))).willReturn(Optional.of(book));
        given(userRepository.findById(ownerUserId)).willReturn(Optional.of(user));

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
        given(reviewRepository.countByBookId(any())).willReturn(0L);
        given(reviewRepository.averageRatingByBookId(any())).willReturn(BigDecimal.ZERO);

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
        given(reviewRepository.countByBookId(any())).willReturn(0L);
        given(reviewRepository.averageRatingByBookId(any())).willReturn(BigDecimal.ZERO);

        reviewService.deleteReview(review.getId(), ownerUserId);

        then(reviewRepository).should().delete(review);
        then(bookService).should().updateReviewData(eq(review.getBookId()), eq(0), eq(new BigDecimal("0.00")));

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
        given(reviewRepository.countByBookId(any())).willReturn(0L);
        given(reviewRepository.averageRatingByBookId(any())).willReturn(BigDecimal.ZERO);

        reviewService.deleteReview(review.getId(), ownerUserId);

        then(reviewLikeRepository).should().deleteAllByReviewId(review.getId());
    }

    @Test
    @DisplayName("좋아요가 없는 상태에서 토글하면 liked=true가 되고, DB의 증가 쿼리를 호출한다")
    void toggleLike_like_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "내용");
        User actor = new User("test@test.com", "닉네임", "password");

        given(userRepository.findById(userId)).willReturn(Optional.of(actor));
        given(reviewRepository.findById(review.getId())).willReturn(Optional.of(review));
        given(reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), userId)).willReturn(false);
        given(reviewRepository.getLikeCount(review.getId())).willReturn(1);

        ReviewLikeResponse response = reviewService.toggleLike(review.getId(), userId);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        then(reviewLikeRepository).should().save(any());
        then(reviewRepository).should().incrementLikeCount(review.getId());
    }

    @Test
    @DisplayName("이미 좋아요 상태에서 토글하면 liked=false가 되고, DB의 감소 쿼리를 호출한다")
    void toggleLike_unlike_success() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "내용");

        given(reviewRepository.findById(review.getId())).willReturn(Optional.of(review));
        given(reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), userId)).willReturn(true);
        given(reviewRepository.getLikeCount(review.getId())).willReturn(0);

        ReviewLikeResponse response = reviewService.toggleLike(review.getId(), userId);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0);
        then(reviewLikeRepository).should().deleteByReviewIdAndUserId(review.getId(), userId);
        then(reviewRepository).should().decrementLikeCount(review.getId());
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
                null, null, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

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
                targetUserId, null, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

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
                null, targetBookId, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).allMatch(r -> r.bookId().equals(targetBookId));
    }

    @Test
    @DisplayName("keyword으로 리뷰를 검색하면 해당 keyword가 포함된 리뷰만 반환한다")
    void getReviews_keyWord() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "좋은 내용이에요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, "좋은", "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).allMatch(r -> r.content().contains("좋은"));
    }

    @Test
    @DisplayName("createdAt 내림차순 정렬로 조회하면 최신 리뷰가 먼저 반환된다")
    void getReviews_createdAt() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

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
                null, null, null, "rating", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

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
                null, null, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of(review1.getId(), review2.getId()));

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).allMatch(r -> r.likedByMe());
    }

    @Test
    @DisplayName("검색 결과가 0건 이면 빈 배열과 hasNext=false를 반환한다")
    void getReviews_emptyResult() {
        UUID requestUserId = UUID.randomUUID();

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt", "DESC", null, null, 50
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of());
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지 조회 시 nextCursor=null, hasNext=false를 반환한다")
    void getReviews_lastPage() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt", "DESC", null, null, 2
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 hasNext=true를 반환한다")
    void getReviews_hasNext() {
        UUID requestUserId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "괜찮아요");
        Review review3 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 2, "별로에요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt", "DESC", null, null, 2
        );

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2, review3));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.content()).hasSize(2);

    }

    @Test
    @DisplayName("cursor/after가 있으면 createdAt 기준 커서 쿼리를 호출한다")
    void getReviews_withCoursor_createdAt() {
        UUID requestUserId = UUID.randomUUID();
        UUID afterId = UUID.randomUUID();
        Instant cursorInstant = Instant.now();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");

        ReviewListRequest request = new ReviewListRequest(
                null, null, null, "createdAt", "DESC",
                cursorInstant.toString(), afterId.toString(), 10
        );

        given(reviewRepository.findReviewsWithCursorCreatedAtDesc(any(), any(),
                any(), any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of(review1));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response =
                reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(1);
        verify(reviewRepository).findReviewsWithCursorCreatedAtDesc(any(), any(),
                any(), any(Instant.class), any(UUID.class), any(Pageable.class));
        verify(reviewRepository, never()).findReviews(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("cursor/after가 있고 orderBy=rating이면 rating 기준 켜서 쿼리를 호출한다")
    void getReviews_withCursor_rating() {
        UUID requestUserId = UUID.randomUUID();
        UUID afterId = UUID.randomUUID();

        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");

        ReviewListRequest request = new ReviewListRequest(null, null, null,
                "rating", "DESC", "4.0", afterId.toString(), 10
        );

        given(reviewRepository.findReviewsWithCursorRatingDesc(any(), any(),
                any(), any(BigDecimal.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of(review1));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response =
                reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(1);
        verify(reviewRepository).findReviewsWithCursorRatingDesc(any(), any(),
                any(), any(BigDecimal.class), any(UUID.class), any(Pageable.class));
        verify(reviewRepository, never()).findReviews(any(), any(), any(),
                any(Pageable.class));

    }

    @Test
    @DisplayName("DAILY period로 조회하면 해당 period의 인기 리뷰 목록을 반환한다")
    void getPopularReview_periodFilter_success() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", new BigDecimal("0.7"), 1, LocalDate.now());

        Review review = Review.create(userId, bookId, 4, "좋은 책이에요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);

        User user = new User("test@test.com", "닉네임", "password");
        ReflectionTestUtils.setField(user, "id", userId);

        given(popularReviewRepository.findByPeriodOrderByRankAsc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));

        given(bookRepository.findAllById(anyList())).willReturn(List.of(book));

        given(userRepository.findAllById(anyList())).willReturn(List.of(user));

        PageResponse<PopularReviewResponse> response = reviewService.getPopularReviews("DAILY", "ASC", null, null, 50);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).period()).isEqualTo("DAILY");
    }

    @Test
    @DisplayName("direction=DESC로 조회하면 findByPeriodOrderByRankDesc를 호출한다")
    void getPopularReview_periodFilter_desc_success() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", new BigDecimal("0.7"), 1, LocalDate.now());

        Review review = Review.create(userId, bookId, 4, "좋은 책이에요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        Book book = Book.builder()
                .title("테스트 책")
                .author("저자")
                .description("설명")
                .publisher("출판사")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);

        User user = new User("test@test.com", "닉네임", "password");
        ReflectionTestUtils.setField(user, "id", userId);

        given(popularReviewRepository.findByPeriodOrderByRankDesc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(popularReview));

        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));
        given(bookRepository.findAllById(anyList())).willReturn(List.of(book));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));

        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews("DAILY", "DESC", null, null, 50);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).period()).isEqualTo("DAILY");
    }

    @Test
    @DisplayName("rank 오름차순으로 조회하면 낮은 rank가 먼저 반환된다")
    void getPopularReviews_sortedByRankAsc() {
        UUID reviewId1 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID bookId1 = UUID.randomUUID();
        UUID reviewId2 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();

        PopularReview rank1 = PopularReview.create(reviewId1, "DAILY", new BigDecimal("1.0"), 1, LocalDate.now());
        PopularReview rank2 = PopularReview.create(reviewId2, "DAILY", new BigDecimal("0.7"), 2, LocalDate.now());

        Review review1 = Review.create(userId1, bookId1, 5, "아주 좋아요");
        ReflectionTestUtils.setField(review1, "id", reviewId1);

        Review review2 = Review.create(userId2, bookId2, 4, "좋아요");
        ReflectionTestUtils.setField(review2, "id", reviewId2);

        Book book1 = Book.builder()
                .title("책1")
                .author("저자1")
                .description("설명1")
                .publisher("출판사1")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        ReflectionTestUtils.setField(book1, "id", bookId1);

        Book book2 = Book.builder()
                .title("책2")
                .author("저자2")
                .description("설명2")
                .publisher("출판사2")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        ReflectionTestUtils.setField(book2, "id", bookId2);

        User user1 = new User("a@a.com", "유저1", "pw");
        ReflectionTestUtils.setField(user1, "id", userId1);

        User user2 = new User("b@b.com", "유저2", "pw");
        ReflectionTestUtils.setField(user2, "id", userId2);

        given(popularReviewRepository.findByPeriodOrderByRankAsc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(rank1, rank2));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review1, review2));

        given(bookRepository.findAllById(anyList())).willReturn(List.of(book1, book2));

        given(userRepository.findAllById(anyList())).willReturn(List.of(user1, user2));
        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews("DAILY", "ASC", null, null, 50);

        assertThat(response.content().get(0).rank()).isEqualTo(1);
        assertThat(response.content().get(1).rank()).isEqualTo(2);

    }

    @Test
    @DisplayName("인기 리뷰 응답에 필요한 필드가 모두 포함된다")
    void getPopularReviews_responseFields() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BigDecimal score = new BigDecimal("0.7");

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", score, 1, LocalDate.now());
        Review review = Review.create(userId, bookId, 4, "좋은 책이에요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        Book book = Book.builder()
                .title("테스트 책").author("저자").description("설명")
                .publisher("출판사").publishedDate(LocalDate.of(2020,1,1))
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);

        User user = new User("test@test.com", "닉네임", "password");
        ReflectionTestUtils.setField(user, "id", userId);

        given(popularReviewRepository.findByPeriodOrderByRankAsc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));
        given(bookRepository.findAllById(anyList())).willReturn(List.of(book));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));

        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews("DAILY", "ASC", null, null, 50);
        PopularReviewResponse result = response.content().get(0);

        assertThat(result.reviewId()).isEqualTo(reviewId);
        assertThat(result.bookId()).isEqualTo(bookId);
        assertThat(result.bookTitle()).isEqualTo("테스트 책");
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userNickname()).isEqualTo("닉네임");
        assertThat(result.reviewContent()).isEqualTo("좋은 책이에요");
        assertThat(result.reviewRating()).isEqualTo(4);
        assertThat(result.period()).isEqualTo("DAILY");
        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(score);
    }

    @Test
    @DisplayName("cursor가 있으면 ASC 방향으로 해당 rank 이후 인기 리뷰를 조회한다")
    void getPopularReviews_withCursor_asc() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BigDecimal score = new BigDecimal("0.7");

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", score, 2, LocalDate.now());
        Review review = Review.create(userId, bookId, 4, "내용");
        ReflectionTestUtils.setField(review, "id", reviewId);
        Book book = Book.builder()
                .title("책")
                .author("저자")
                .description("설명")
                .publisher("출판사")
                .publishedDate(LocalDate.of(2020, 1, 1))
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);
        User user = new User("test@test.com", "닉네임", "password");
        ReflectionTestUtils.setField(user, "id", userId);

        given(popularReviewRepository.findByPeriodAndRankGreaterThan(eq("DAILY"), eq(1), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));
        given(bookRepository.findAllById(anyList())).willReturn(List.of(book));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));

        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews("DAILY", "ASC", "1", null, 50);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("cursor가 있으면 DESC 방향으로 해당 rank 이전 인기 리뷰를 조회한다")
    void getPopularReviews_withCursor_desc() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BigDecimal score = new BigDecimal("0.5");

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", score, 1, LocalDate.now());
        Review review = Review.create(userId, bookId, 3, "내용");
        ReflectionTestUtils.setField(review, "id", reviewId);
        Book book = Book.builder()
                .title("책")
                .author("저자")
                .description("설명")
                .publisher("출판사")
                .publishedDate(LocalDate.of(2020,1,1))
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);
        User user = new User("test@test.com", "닉네임", "password");
        ReflectionTestUtils.setField(user, "id", userId);

        given(popularReviewRepository.findByPeriodAndRankLessThan(eq("DAILY"), eq(3), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));
        given(bookRepository.findAllById(anyList())).willReturn(List.of(book));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));

        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews("DAILY", "DESC", "3", null, 50);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).rank()).isEqualTo(1);
    }


    @Test
    @DisplayName("incrementCommentCount 호출 시 repository 쿼리를 실행한다")
    void incrementCommentCount_success() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.incrementCommentCount(reviewId)).willReturn(1);

        reviewService.incrementCommentCount(reviewId);

        verify(reviewRepository).incrementCommentCount(reviewId);
    }

    @Test
    @DisplayName("decrementCommentCount 호출 시 repository 쿼리를 실행한다")
    void decrementCommentCount_success() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.decrementCommentCount(reviewId)).willReturn(1);

        reviewService.decrementCommentCount(reviewId);

        verify(reviewRepository).decrementCommentCount(reviewId);
    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 incrementCommentCount 호출 시 예외가 발생한다")
    void incrementCommentCount_reviewNotFound() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.incrementCommentCount(reviewId)).willReturn(0);

        assertThatThrownBy(() -> reviewService.incrementCommentCount(reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 reviewId로 decrementCommentCount 호출 시 예외가 발생한다")
    void decrementCommentCount_reviewNotFound() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.decrementCommentCount(reviewId)).willReturn(0);
        given(reviewRepository.existsById(reviewId)).willReturn(false);

        assertThatThrownBy(() -> reviewService.decrementCommentCount(reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("commentCount가 0인 리뷰에 decrementCommentCount 호출 시 예외없이 종료된다")
    void decrementCommentCount_alreadyZero() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.decrementCommentCount(reviewId)).willReturn(0);
        given(reviewRepository.existsById(reviewId)).willReturn(true);

        assertThatCode(() -> reviewService.decrementCommentCount(reviewId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cursor/after가 있고 orderBy=rating, direction=ASC이면 rating ASC 커서 쿼리를 호출한다")
    void getReviews_withCursor_ratingAsc() {
        UUID requestUserId = UUID.randomUUID();
        UUID afterId = UUID.randomUUID();
        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");

        ReviewListRequest request = new ReviewListRequest(null, null, null,
                "rating", "ASC", "4.0", afterId.toString(), 10);

        given(reviewRepository.findReviewsWithCursorRatingAsc(any(), any(), any(),
                any(BigDecimal.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of(review1));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(1);
        verify(reviewRepository).findReviewsWithCursorRatingAsc(any(), any(), any(),
                any(BigDecimal.class), any(UUID.class), any(Pageable.class));
        verify(reviewRepository, never()).findReviews(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("cursor/after가 있고 orderBy=createdAt, direction=ASC이면 createdAt ASC 커서 쿼리를 호출한다")
    void getReviews_withCursor_createdAsc() {
        UUID requestUserId = UUID.randomUUID();
        UUID afterId = UUID.randomUUID();
        Instant cursorInstant = Instant.now();
        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책이에요");

        ReviewListRequest request = new ReviewListRequest(null, null, null,
                "createdAt", "ASC", cursorInstant.toString(), afterId.toString(), 10);

        given(reviewRepository.findReviewsWithCursorCreatedAtAsc(any(), any(), any(),
                any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of(review1));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId, request);

        assertThat(response.content()).hasSize(1);
        verify(reviewRepository).findReviewsWithCursorCreatedAtAsc(any(), any(), any(),
                any(Instant.class), any(UUID.class), any(Pageable.class));
        verify(reviewRepository, never()).findReviews(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("rating 기준 다음 페이지가 있으면 nextCursor에 마지막 항목의 rating 값이 설정된다")
    void getReviews_hasNext_withRatingCursor() {
        UUID requestUserId = UUID.randomUUID();
        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 5, "첫 번째");
        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "두 번째");
        Review review3 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1, "세 번째");

        ReviewListRequest request = new ReviewListRequest(null, null, null,
                "rating", "DESC", null, null, 2);

        given(reviewRepository.findReviews(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(review1, review2, review3));
        given(reviewLikeRepository.findLikedReviewIds(any(), any()))
                .willReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviews(requestUserId,request);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(String.valueOf(review2.getRating()));
        assertThat(response.nextAfter()).isEqualTo(review2.getId().toString());

    }

    @Test
    @DisplayName("인기 리뷰 조회 시 reviewMap에 없는 항목은 결과에서 제외된다")
    void getPopularReviews_nullReview_filtered() {
        UUID reviewId = UUID.randomUUID();
        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", new BigDecimal("0.7"), 1, LocalDate.now());

        given(popularReviewRepository.findByPeriodOrderByRankAsc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList()))
                .willReturn(List.of());

        PageResponse<PopularReviewResponse> response = reviewService.getPopularReviews("DAILY", "ASC", null, null, 50);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("인기 리뷰 조회 시 bookMap 또는 userMap에 없는 항목은 결과에서 제외된다")
    void getPopularReviews_nullBookOrUser_filtered() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        PopularReview popularReview = PopularReview.create(reviewId, "DAILY", new BigDecimal("0.7"), 1, LocalDate.now());

        Review review = Review.create(userId, bookId, 4, "내용");

        ReflectionTestUtils.setField(review, "id", reviewId);

        given(popularReviewRepository.findByPeriodOrderByRankAsc(eq("DAILY"), any(Pageable.class)))
                .willReturn(List.of(popularReview));
        given(reviewRepository.findAllById(anyList())).willReturn(List.of(review));
        given(bookRepository.findAllById(anyList())).willReturn(List.of());

        PageResponse<PopularReviewResponse> response = reviewService.getPopularReviews("DAILY", "ASC",
                null, null, 50);

        assertThat(response.content()).isEmpty();
    }

}
