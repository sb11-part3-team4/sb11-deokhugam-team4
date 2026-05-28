package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentRequiredException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentTooLongException;
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
    }

    @Test
    @DisplayName("내용이 빈 문자열이면 예외가 발생한다")
    void createComment_emptyContent_throwsException() {
        String content = "";

        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, content))
                .isInstanceOf(CommentContentRequiredException.class);
    }

    @Test
    @DisplayName("내용이 null이면 예외가 발생한다")
    void createComment_nullContent_throwsException() {
        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, null))
                .isInstanceOf(CommentContentRequiredException.class);
    }

    @Test
    @DisplayName("내용이 최대 글자수를 초과하면 예외가 발생한다")
    void createComment_contentExceedsMaxLength_throwsException() {
        String content = "a".repeat(Comment.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, content))
                .isInstanceOf(CommentContentTooLongException.class);
    }

    @Test
    @DisplayName("내용이 공백 문자열이면 예외가 발생한다")
    void createComment_blankContent_throwsException() {
        String content = "   ";

        assertThatThrownBy(() -> Comment.create(REVIEW_ID, USER_ID, content))
                .isInstanceOf(CommentContentRequiredException.class);
    }

    @Test
    @DisplayName("내용이 최대 글자수와 정확히 같으면 정상 생성된다")
    void createComment_maxLengthContent_success() {
        String content = "a".repeat(Comment.MAX_CONTENT_LENGTH);

        Comment comment = Comment.create(REVIEW_ID, USER_ID, content);

        assertThat(comment.getContent()).hasSize(Comment.MAX_CONTENT_LENGTH);
    }

    @Test
    @DisplayName("내용을 정상적으로 수정한다")
    void updateContent_success() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "원본 댓글입니다.");
        String updatedContent = "수정된 댓글입니다.";

        comment.updateContent(updatedContent);

        assertThat(comment.getContent()).isEqualTo(updatedContent);
    }

    @Test
    @DisplayName("수정 내용이 비어있으면 예외가 발생한다")
    void updateContent_emptyContent_throwsException() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "원본 댓글입니다.");

        assertThatThrownBy(() -> comment.updateContent(""))
                .isInstanceOf(CommentContentRequiredException.class);
    }

    @Test
    @DisplayName("수정 내용이 최대 글자수를 초과하면 예외가 발생한다")
    void updateContent_contentExceedsMaxLength_throwsException() {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, "원본 댓글입니다.");
        String tooLongContent = "a".repeat(Comment.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> comment.updateContent(tooLongContent))
                .isInstanceOf(CommentContentTooLongException.class);
    }
}