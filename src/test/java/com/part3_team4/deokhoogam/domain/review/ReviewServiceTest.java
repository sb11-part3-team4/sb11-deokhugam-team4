package com.part3_team4.deokhoogam.domain.review;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.review.service.ReviewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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

        ReviewResponse response = reviewService.getReview(review.getId(),userId);

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










}
