package com.part3_team4.deokhoogam.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "popular_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularReview {
    @Id
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "period", nullable = false)
    private String period;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static PopularReview create(UUID reviewId, String period, BigDecimal score, int rank, LocalDate baseDate) {
        PopularReview popularReview = new PopularReview();
        popularReview.id = UUID.randomUUID();
        popularReview.reviewId = reviewId;
        popularReview.period = period;
        popularReview.score = score;
        popularReview.rank = rank;
        popularReview.baseDate= baseDate;
        popularReview.createdAt= Instant.now();
        return popularReview;
    }



}
