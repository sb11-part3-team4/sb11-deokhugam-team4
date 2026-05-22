package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("댓글을 정상적으로 생성한다")
    void createComment_success() {
        String content = "정말 좋은 리뷰입니다.";

        Comment comment = Comment.create(REVIEW_ID, USER_ID, content);

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(comment.getUserId()).isEqualTo(USER_ID);
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("내용이 빈 문자열이면 예외가 발생한다")
    void createComment_emptyContent_throwsException() {
        String content = "";

        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, content))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("내용이 null이면 예외가 발생한다")
    void createComment_nullContent_throwsException() {
        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, null))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("내용이 최대 글자수를 초과하면 예외가 발생한다")
    void createComment_contentExceedsMaxLength_throwsException() {
        String content = "a".repeat(Comment.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, content))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("논리 삭제 시 deletedAt이 설정된다")
    void softDelete_setsDeletedAt() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "댓글 내용입니다.");

        comment.softDelete();

        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("물리 삭제는 UI 없이 테스트 코드로만 검증한다")
    void hardDelete_notExposedInUI() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "물리 삭제 대상 댓글입니다.");

        // 실제 물리 삭제(repository.delete())는 CommentRepositoryTest에서 검증
        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getDeletedAt()).isNull();
    }
}