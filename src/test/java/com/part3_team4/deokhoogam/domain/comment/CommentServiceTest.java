package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.comment.service.CommentServiceImpl;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private DeletedCommentRepository deletedCommentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMMENT_ID = UUID.randomUUID();

    // ─────────────────────────────────────────────
    // 댓글 등록
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 등록")
    class CreateComment {

        @Test
        @DisplayName("댓글을 정상적으로 등록한다")
        void createComment_success() {
            String content = "정말 좋은 리뷰입니다.";
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(userRepository.existsById(USER_ID)).willReturn(true);
            Comment saved = Comment.create(REVIEW_ID, USER_ID, content);
            given(commentRepository.save(any(Comment.class))).willReturn(saved);

            CommentDto.CommentResponse response = commentService.createComment(REVIEW_ID, USER_ID, content);

            assertThat(response.id()).isNotNull();
            assertThat(response.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.content()).isEqualTo(content);
            then(commentRepository).should().save(any(Comment.class));
            // TODO: 알림 담당 팀원 코드 연결 후 구현
        }

        @Test
        @DisplayName("존재하지 않는 리뷰에 댓글을 등록하면 예외가 발생한다")
        void createComment_reviewNotFound_throwsException() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(false);

            // TODO: Review 도메인 팀 작업 후 ReviewNotFoundException으로 교체
            assertThatThrownBy(() -> commentService.createComment(REVIEW_ID, USER_ID, "내용"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저가 댓글을 등록하면 예외가 발생한다")
        void createComment_userNotFound_throwsException() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(userRepository.existsById(USER_ID)).willReturn(false);

            // TODO: User 도메인 팀 작업 후 UserNotFoundException으로 교체
            assertThatThrownBy(() -> commentService.createComment(REVIEW_ID, USER_ID, "내용"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 수정
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("본인이 작성한 댓글을 정상적으로 수정한다")
        void updateComment_success() {
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "기존 내용");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentDto.CommentResponse response = commentService.updateComment(COMMENT_ID, USER_ID, "수정된 내용");

            assertThat(response.content()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("다른 사람의 댓글을 수정하면 예외가 발생한다")
        void updateComment_notOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "기존 내용");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, otherUserId, "수정된 내용"))
                    .isInstanceOf(CommentNotOwnerException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 수정하면 예외가 발생한다")
        void updateComment_notFound_throwsException() {
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, USER_ID, "수정된 내용"))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 논리 삭제
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 논리 삭제")
    class SoftDeleteComment {

        @Test
        @DisplayName("본인이 작성한 댓글을 정상적으로 논리 삭제한다 (comment → deleted_comment 이동)")
        void softDeleteComment_success() {
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "삭제할 댓글입니다.");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            commentService.softDeleteComment(COMMENT_ID, USER_ID);

            InOrder inOrder = inOrder(deletedCommentRepository, commentRepository);
            then(deletedCommentRepository).should(inOrder).save(any(DeletedComment.class));
            then(commentRepository).should(inOrder).delete(comment);
        }

        @Test
        @DisplayName("다른 사람의 댓글을 삭제하면 예외가 발생한다")
        void softDeleteComment_notOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "삭제할 댓글입니다.");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.softDeleteComment(COMMENT_ID, otherUserId))
                    .isInstanceOf(CommentNotOwnerException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하면 예외가 발생한다")
        void softDeleteComment_notFound_throwsException() {
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.softDeleteComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 물리 삭제
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 물리 삭제")
    class HardDeleteComment {

        @Test
        @DisplayName("deleted_comment를 정상적으로 물리 삭제한다")
        void hardDeleteComment_success() {
            DeletedComment deletedComment = DeletedComment.from(Comment.create(REVIEW_ID, USER_ID, "물리 삭제될 댓글입니다."));
            given(deletedCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(deletedComment));

            commentService.hardDeleteComment(COMMENT_ID);

            then(deletedCommentRepository).should().delete(deletedComment);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 물리 삭제하면 예외가 발생한다")
        void hardDeleteComment_notFound_throwsException() {
            given(deletedCommentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.hardDeleteComment(COMMENT_ID))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 목록 조회
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("review_id로 댓글 목록을 시간 역순(createdAt DESC)으로 조회한다")
        void getComments_byReviewId_orderedByCreatedAt() {
            Comment comment1 = Comment.create(REVIEW_ID, USER_ID, "첫 번째 댓글");
            Comment comment2 = Comment.create(REVIEW_ID, USER_ID, "두 번째 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of(comment1, comment2));

            List<CommentDto.CommentResponse> result = commentService.getComments(REVIEW_ID, null, 10);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).content()).isEqualTo("첫 번째 댓글");
            assertThat(result.get(1).content()).isEqualTo("두 번째 댓글");
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 정상적으로 동작한다")
        void getComments_withCursor_returnsPaginatedResults() {
            Instant cursor = Instant.now().minusSeconds(1);
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "커서 이전 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    eq(REVIEW_ID), eq(cursor), any(Pageable.class)))
                    .willReturn(List.of(comment));

            List<CommentDto.CommentResponse> result = commentService.getComments(REVIEW_ID, cursor, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).content()).isEqualTo("커서 이전 댓글");
        }

        @Test
        @DisplayName("결과 없을 때 빈 리스트를 반환한다")
        void getComments_noResults_returnsEmptyList() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of());

            List<CommentDto.CommentResponse> result = commentService.getComments(REVIEW_ID, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 reviewId로 조회하면 예외가 발생한다")
        void getComments_reviewNotFound_throwsException() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(false);

            // TODO: Review 도메인 팀 작업 후 ReviewNotFoundException으로 교체
            assertThatThrownBy(() -> commentService.getComments(REVIEW_ID, null, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 상세 조회
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 상세 조회")
    class GetComment {

        @Test
        @DisplayName("commentId로 댓글 상세 정보를 조회한다")
        void getComment_success() {
            Comment comment = Comment.create(REVIEW_ID, USER_ID, "상세 조회할 댓글입니다.");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentDto.CommentResponse response = commentService.getComment(COMMENT_ID);

            assertThat(response.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.content()).isEqualTo("상세 조회할 댓글입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 조회하면 예외가 발생한다")
        void getComment_notFound_throwsException() {
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getComment(COMMENT_ID))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}
