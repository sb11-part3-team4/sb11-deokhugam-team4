package com.part3_team4.deokhoogam.domain.comment.entity;

import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentRequiredException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentTooLongException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentInvalidArgumentException;
import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    // NOTE: Review 도메인과 aggregate 경계를 분리합니다.
    // TODO: User, Review 엔티티 머지 후 @ManyToOne으로 교체
    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    // TODO: User, Review 엔티티 머지 후 @ManyToOne으로 교체
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    private Comment(UUID reviewId, UUID userId, String content) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.content = content;
    }

    public static Comment create(UUID reviewId, UUID userId, String content) {
        if (reviewId == null || userId == null) {
            throw CommentInvalidArgumentException.create();
        }
        validate(content);
        return new Comment(reviewId, userId, content);
    }

    public void updateContent(String content) {
        validate(content);
        this.content = content;
    }

    private static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw CommentContentRequiredException.create();
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw CommentContentTooLongException.create();
        }
    }
}