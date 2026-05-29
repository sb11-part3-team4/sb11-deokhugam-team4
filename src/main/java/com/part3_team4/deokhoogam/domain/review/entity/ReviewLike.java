package com.part3_team4.deokhoogam.domain.review.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLike {

    @Id
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ReviewLike create(UUID reviewId, UUID userId) {
        ReviewLike reviewLike = new ReviewLike();
        reviewLike.id = UUID.randomUUID();
        reviewLike.reviewId = reviewId;
        reviewLike.userId = userId;
        reviewLike.createdAt = Instant.now();
        return reviewLike;

    }
}
