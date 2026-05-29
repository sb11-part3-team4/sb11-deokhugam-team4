package com.part3_team4.deokhoogam.domain.review.controller;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("Deokhugam-Request-User-ID")
            UUID userId,
            @PathVariable
            UUID reviewId
    ) {
       ReviewResponse response = reviewService.getReview(reviewId, userId);
       return ResponseEntity.ok(response);
    }

}
