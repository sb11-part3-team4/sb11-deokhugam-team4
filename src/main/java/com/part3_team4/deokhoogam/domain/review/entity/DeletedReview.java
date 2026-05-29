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
@Table(name = "deleted_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletedReview {

    @Id
    private UUID id;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    public static DeletedReview from(Review review) {
        DeletedReview deletedReview = new DeletedReview();
        deletedReview.id = review.getId();
        deletedReview.userId = review.getUserId();
        deletedReview.bookId = review.getBookId();
        deletedReview.rating = review.getRating();
        deletedReview.content = review.getContent();
        deletedReview.likeCount = review.getLikeCount();
        deletedReview.commentCount = review.getCommentCount();
        deletedReview.createdAt = review.getCreatedAt();
        deletedReview.updatedAt = review.getUpdatedAt();
        deletedReview.deletedAt = Instant.now();
        return deletedReview;
    }
}
