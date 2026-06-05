package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotOwnerException;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;

import com.part3_team4.deokhoogam.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return new ReviewLikeResponse(reviewId, !alreadyLiked, review.getLikeCount());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(UUID userId, ReviewListRequest request)
    {

        Sort sort = Sort.by(Sort.Direction.fromString(request.direction()),
        request.orderBy());
        Pageable pageable = PageRequest.of(0, request.limit() + 1, sort);
        List<Review> reviews = reviewRepository.findReviews(request.userId(), request.bookId(), request.keyword(), pageable);

        boolean hasNext = reviews.size() > request.limit();
        if (hasNext) {
            reviews = reviews.subList(0, request.limit());
        }

        List<ReviewResponse> content = reviews.stream()
                .map(review -> {
                    boolean likedByMe =
                reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), userId);
                    return new ReviewResponse(
                            review.getId(), review.getUserId(), review.getBookId(),
                            review.getRating(), review.getContent(),
                            review.getLikeCount(), review.getCommentCount(),
                            likedByMe, review.getCreatedAt(), review.getUpdatedAt()
                    );
                })
                .toList();

        String nextCursor = null;
        String nextAfter = null;
        if (hasNext && !content.isEmpty()) {
            ReviewResponse last = content.get(content.size() - 1);
            nextCursor = "rating".equals(request.orderBy())
                    ? String.valueOf(last.rating())
                    : last.createdAt().toString();
            nextAfter = last.id().toString();
        }

        return new PageResponse<>(content, nextCursor, nextAfter, content.size(), null, hasNext);
    }

}