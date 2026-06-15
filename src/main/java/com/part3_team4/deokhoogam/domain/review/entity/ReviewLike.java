package com.part3_team4.deokhoogam.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_like", uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_like_user", columnNames = {"review_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ReviewLike create(UUID reviewId, UUID userId) {
        ReviewLike reviewLike = new ReviewLike();
        reviewLike.reviewId = reviewId;
        reviewLike.userId = userId;
        reviewLike.createdAt = Instant.now();
        return reviewLike;
    }
}