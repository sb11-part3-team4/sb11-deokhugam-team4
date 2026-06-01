package com.part3_team4.deokhoogam.domain.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentInvalidArgumentException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deleted_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletedComment {

    @Id
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = Comment.MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    private DeletedComment(UUID id, UUID reviewId, UUID userId, String content,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = Instant.now();
    }

    public static DeletedComment from(Comment comment) {
        if (comment == null) {
            throw CommentInvalidArgumentException.create();
        }
        return new DeletedComment(
                comment.getId(),
                comment.getReviewId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}