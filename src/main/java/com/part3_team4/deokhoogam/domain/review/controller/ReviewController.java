package com.part3_team4.deokhoogam.domain.review.controller;

import com.part3_team4.deokhoogam.domain.review.dto.*;
import com.part3_team4.deokhoogam.domain.review.service.ReviewService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

}
