package com.part3_team4.deokhoogam.domain.review.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    public static Review create(UUID userId, UUID bookId, int rating, String content) {
        Review review = new Review();
        review.userId = userId;
        review.bookId = bookId;
        review.rating = rating;
        review.content = content;
        review.likeCount = 0;
        review.commentCount = 0;
        return review;
    }
}
