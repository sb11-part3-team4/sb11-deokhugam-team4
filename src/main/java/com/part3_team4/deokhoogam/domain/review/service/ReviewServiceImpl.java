package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID userId, ReviewCreateRequest request) {

        bookRepository.findById(request.bookId())
                .orElseThrow(() -> BookNotFoundException.withId(request.bookId()));

        if (reviewRepository.existsByUserIdAndBookId(userId, request.bookId())) {
            throw ReviewAlreadyExistsException.withUserIdAndBookId(userId, request.bookId());
        }

        Review review = Review.create(userId, request.bookId(), request.rating(), request.content());
        Review saved;
        try {
            saved = reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw ReviewAlreadyExistsException.withUserIdAndBookId(userId, request.bookId());
        }

        return new ReviewResponse(
                saved.getId(), saved.getUserId(), saved.getBookId(),
                saved.getRating(), saved.getContent(),
                saved.getLikeCount(), saved.getCommentCount(),
                false, saved.getCreatedAt(), saved.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->ReviewNotFoundException.withId(reviewId));

        boolean likedByMe =
    reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        return new ReviewResponse(
                review.getId(), review.getUserId(), review.getBookId(),
                review.getRating(), review.getContent(),
                review.getLikeCount(), review.getCommentCount(),
                likedByMe, review.getCreatedAt(), review.getUpdatedAt()
        );
    }


}
