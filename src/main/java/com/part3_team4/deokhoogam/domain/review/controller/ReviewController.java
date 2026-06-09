package com.part3_team4.deokhoogam.domain.review.controller;

import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.service.ReviewService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.domain.review.dto.PopularReviewResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @RequestBody @Valid ReviewCreateRequest request
    ) {
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @PathVariable UUID reviewId
    ) {
       ReviewResponse response = reviewService.getReview(reviewId, userId);
       return ResponseEntity.ok(response);
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewUpdateRequest request
            ) {
        ReviewResponse response = reviewService.updateReview(reviewId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @PathVariable UUID reviewId
    ) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<ReviewLikeResponse> toggleLike(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @PathVariable UUID reviewId
    ) {
        ReviewLikeResponse response = reviewService.toggleLike(reviewId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @RequestHeader("Deokhugam-Request-User-ID") UUID userId,
            @RequestParam(required = false) UUID filterUserId,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) String keyword,
            @Pattern(regexp = "createdAt|rating") @RequestParam(defaultValue = "createdAt") String orderBy,
            @Pattern(regexp = "ASC|DESC") @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String after,
            @Min(1) @Max(100) @RequestParam(defaultValue = "50") int limit
    ) {
        ReviewListRequest request = new ReviewListRequest(
                filterUserId, bookId, keyword, orderBy, direction, cursor, after, limit
        );
        PageResponse<ReviewResponse> response = reviewService.getReviews(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    public ResponseEntity<PageResponse<PopularReviewResponse>> getPopularReviews(
            @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|ALL_TIME") @RequestParam(defaultValue = "DAILY") String period,
            @Pattern(regexp = "ASC|DESC") @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String after,
            @Min(1) @Max(100) @RequestParam(defaultValue = "50") int limit
    ) {
        PageResponse<PopularReviewResponse> response =
                reviewService.getPopularReviews(period, direction, cursor, after, limit);
        return ResponseEntity.ok(response);
    }

}
