package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentInvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeletedCommentTest {

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Comment로부터 DeletedComment로 정상 변환된다")
    void from_comment_success() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "삭제할 댓글입니다.");

        Instant before = Instant.now();
        DeletedComment deletedComment = DeletedComment.from(comment);
        Instant after = Instant.now();

        assertThat(deletedComment.getId()).isEqualTo(comment.getId());
        assertThat(deletedComment.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(deletedComment.getUserId()).isEqualTo(USER_ID);
        assertThat(deletedComment.getContent()).isEqualTo("삭제할 댓글입니다.");
        // persist 전이므로 null임을 명시
        assertThat(deletedComment.getCreatedAt()).isNull();
        assertThat(deletedComment.getUpdatedAt()).isNull();
        // deletedAt 시간 범위 검증
        assertThat(deletedComment.getDeletedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("comment가 null이면 예외가 발생한다")
    void from_nullComment_throwsException() {
        assertThatThrownBy(() -> DeletedComment.from(null))
                .isInstanceOf(CommentInvalidArgumentException.class);
    }
}