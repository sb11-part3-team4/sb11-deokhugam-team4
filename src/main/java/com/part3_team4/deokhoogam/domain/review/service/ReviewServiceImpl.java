package com.part3_team4.deokhoogam.domain.review.service;

import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
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
}
