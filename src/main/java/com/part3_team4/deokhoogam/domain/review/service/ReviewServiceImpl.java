package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotOwnerException;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;

import java.util.*;

import com.part3_team4.deokhoogam.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.part3_team4.deokhoogam.domain.review.entity.QReview.review;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final DeletedReviewRepository deletedReviewRepository;
    private final PopularReviewRepository popularReviewRepository;
    private final UserRepository userRepository;

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

        List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
        Set<UUID> likedReviewIds = new HashSet<>(
                reviewLikeRepository.findLikedReviewIds(userId, reviewIds));

        List<ReviewResponse> content = reviews.stream()
                .map(review -> new ReviewResponse(
                        review.getId(), review.getUserId(), review.getBookId(),
                        review.getRating(), review.getContent(),
                        review.getLikeCount(), review.getCommentCount(),
                        likedReviewIds.contains(review.getId()),
                        review.getCreatedAt(), review.getUpdatedAt()
                ))
                .toList();

        String nextCursor = null;
        String nextAfter = null;
        if (hasNext && !content.isEmpty()) {
            ReviewResponse last = content.get(content.size() - 1);
            nextCursor = "rating".equals(request.orderBy())
                    ? String.valueOf(last.rating())
                    : last.createdAt() != null ? last.createdAt().toString() : null;
            nextAfter = last.id().toString();
        }

        return new PageResponse<>(content, nextCursor, nextAfter, content.size(), null, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PopularReviewResponse> getPopularReviews(String period, String direction, String cursor, String after, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.fromString(direction), "rank"));
        List<PopularReview> popularReviews = popularReviewRepository.findByPeriod(period, pageable);

        boolean hasNext = popularReviews.size() > limit;
        if (hasNext) {
            popularReviews = popularReviews.subList(0, limit);
        }

        List<PopularReviewResponse> content = popularReviews.stream()
                .map(pr -> reviewRepository.findById(pr.getReviewId())
                        .map(review -> toPopularReviewResponse(pr, review))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();


        String nextCursor = null;
        String nextAfter = null;
        if (hasNext && !content.isEmpty()) {
            PopularReviewResponse last = content.get(content.size() - 1);
            nextCursor = String.valueOf(last.rank());
            nextAfter = last.createdAt().toString();
        }

        return new PageResponse<>(content, nextCursor, nextAfter, content.size(), null, hasNext);
    }

    private PopularReviewResponse toPopularReviewResponse(PopularReview pr, Review review) {
        var book = bookRepository.findById(review.getBookId())
                .orElseThrow(() -> BookNotFoundException.withId(review.getBookId()));
        var user = userRepository.findById(review.getUserId())
                .orElseThrow(() -> UserNotFoundException.withId(review.getUserId()));
        return new PopularReviewResponse(
                pr.getId(), pr.getReviewId(), review.getBookId(),
                book.getTitle(), book.getThumbnailUrl(),
                review.getUserId(), user.getName(),
                review.getContent(), review.getRating(),
                pr.getPeriod(), pr.getCreatedAt(),
                pr.getRank(), pr.getScore(),
                review.getLikeCount(), review.getCommentCount()
        );
    }

}