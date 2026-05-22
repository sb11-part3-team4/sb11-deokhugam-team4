package com.part3_team4.deokhoogam.domain.comment.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Comment(UUID reviewId, UUID userId, String content) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.content = content;
    }

    public static Comment create(UUID reviewId, UUID userId, String content) {
        validate(content);
        return new Comment(reviewId, userId, content);
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    private static void validate(String content) {
        if (content == null || content.isBlank()) {
            // TODO: ErrorCode.COMMENT_CONTENT_REQUIRED 로 교체
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            // TODO: ErrorCode.COMMENT_CONTENT_TOO_LONG 으로 교체
            throw new IllegalArgumentException("댓글 내용은 최대 " + MAX_CONTENT_LENGTH + "자까지 입력 가능합니다.");
        }
    }
}