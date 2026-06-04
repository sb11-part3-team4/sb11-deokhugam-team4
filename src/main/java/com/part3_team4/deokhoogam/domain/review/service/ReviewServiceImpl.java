package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewLikeResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewUpdateRequest;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotOwnerException;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
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
    private final DeletedReviewRepository deletedReviewRepository;

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
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

        boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        return new ReviewResponse(
                review.getId(), review.getUserId(), review.getBookId(),
                review.getRating(), review.getContent(),
                review.getLikeCount(), review.getCommentCount(),
                likedByMe, review.getCreatedAt(), review.getUpdatedAt()
        );
    }


    @Override
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

        if (!review.getUserId().equals(userId)) {
            throw ReviewNotOwnerException.withUserId(userId);
        }

        review.update(request.rating(), request.content());

        boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        return new ReviewResponse(
                review.getId(), review.getUserId(), review.getBookId(), review.getRating(), review.getContent(),
                review.getLikeCount(), review.getCommentCount(), likedByMe, review.getCreatedAt(), review.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));
        if (!review.getUserId().equals(userId))
            throw ReviewNotOwnerException.withUserId(userId);
        reviewLikeRepository.deleteAllByReviewId(reviewId);
        deletedReviewRepository.save(DeletedReview.from(review));
        reviewRepository.delete(review);
    }

    @Override
    @Transactional
    public ReviewLikeResponse toggleLike(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

        boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
        if (alreadyLiked) {
            reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            review.decrementLikeCount();
        } else {
            reviewLikeRepository.save(ReviewLike.create(reviewId, userId));
            review.incrementLikeCount();
        }
        return new ReviewLikeResponse(reviewId, alreadyLiked, review.getLikeCount());
    }


}