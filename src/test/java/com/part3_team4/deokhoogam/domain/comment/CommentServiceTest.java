package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.comment.service.CommentServiceImpl;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

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

    @Mock
    private NotificationService notificationService;

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
        @DisplayName("댓글을 정상적으로 등록하고 알림을 생성한다")
        void createComment_success() {
            String content = "정말 좋은 리뷰입니다.";
            UUID reviewOwnerId = UUID.randomUUID();
            Review mockReview = mock(Review.class);
            given(mockReview.getId()).willReturn(REVIEW_ID);
            given(mockReview.getUserId()).willReturn(reviewOwnerId);
            given(mockReview.getContent()).willReturn("리뷰 내용");
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(mockUser.getName()).willReturn("테스트유저");

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(mockReview));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            Comment saved = Comment.create(mockReview, mockUser, content);
            given(commentRepository.save(any(Comment.class))).willReturn(saved);

            CommentDto.CommentResponse response = commentService.createComment(REVIEW_ID, USER_ID, content);

            assertThat(response.id()).isNotNull();
            assertThat(response.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.content()).isEqualTo(content);
            assertThat(response.userNickname()).isEqualTo("테스트유저");
            then(commentRepository).should().save(any(Comment.class));
            then(notificationService).should().createNotification(
                    eq(reviewOwnerId), eq(REVIEW_ID), eq("리뷰 내용"), eq("테스트유저"), eq(USER_ID));
        }

        @Test
        @DisplayName("알림 생성이 실패해도 댓글 등록은 성공한다")
        void createComment_notificationFailure_commentStillCreated() {
            String content = "정말 좋은 리뷰입니다.";
            Review mockReview = mock(Review.class);
            given(mockReview.getId()).willReturn(REVIEW_ID);
            given(mockReview.getUserId()).willReturn(UUID.randomUUID());
            given(mockReview.getContent()).willReturn("리뷰 내용");
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(mockUser.getName()).willReturn("테스트유저");

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(mockReview));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            Comment saved = Comment.create(mockReview, mockUser, content);
            given(commentRepository.save(any(Comment.class))).willReturn(saved);
            willThrow(new RuntimeException("알림 서버 오류"))
                    .given(notificationService).createNotification(any(), any(), any(), any(), any());

            CommentDto.CommentResponse response = commentService.createComment(REVIEW_ID, USER_ID, content);

            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo(content);
            then(commentRepository).should().save(any(Comment.class));
        }

        @Test
        @DisplayName("존재하지 않는 리뷰에 댓글을 등록하면 예외가 발생한다")
        void createComment_reviewNotFound_throwsException() {
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(REVIEW_ID, USER_ID, "내용"))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저가 댓글을 등록하면 예외가 발생한다")
        void createComment_userNotFound_throwsException() {
            Review mockReview = mock(Review.class);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(mockReview));
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(REVIEW_ID, USER_ID, "내용"))
                    .isInstanceOf(UserNotFoundException.class);
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
            Review mockReview = mock(Review.class);
            given(mockReview.getId()).willReturn(REVIEW_ID);
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(mockUser.getName()).willReturn("테스트유저");

            Comment comment = Comment.create(mockReview, mockUser, "기존 내용");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentDto.CommentResponse response = commentService.updateComment(COMMENT_ID, USER_ID, "수정된 내용");

            assertThat(response.content()).isEqualTo("수정된 내용");
            assertThat(response.userNickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("다른 사람의 댓글을 수정하면 예외가 발생한다")
        void updateComment_notOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            Review mockReview = mock(Review.class);
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);

            Comment comment = Comment.create(mockReview, mockUser, "기존 내용");
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
            Review mockReview = mock(Review.class);
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);

            Comment comment = Comment.create(mockReview, mockUser, "삭제할 댓글입니다.");
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
            Review mockReview = mock(Review.class);
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);

            Comment comment = Comment.create(mockReview, mockUser, "삭제할 댓글입니다.");
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
            Review mockReview = mock(Review.class);
            User mockUser = mock(User.class);

            DeletedComment deletedComment = DeletedComment.from(Comment.create(mockReview, mockUser, "물리 삭제될 댓글입니다."));
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

        private Review testReview() {
            Review r = mock(Review.class);
            given(r.getId()).willReturn(REVIEW_ID);
            return r;
        }

        private User testUser() {
            User u = mock(User.class);
            given(u.getId()).willReturn(USER_ID);
            given(u.getName()).willReturn("테스트유저");
            return u;
        }

        @Test
        @DisplayName("direction=DESC, cursor 없으면 시간 역순 첫 페이지를 닉네임과 함께 반환한다")
        void getComments_desc_noCursor_returnsFirstPage() {
            User mockUser = testUser();
            Review mockReview = testReview();
            Comment comment1 = Comment.create(mockReview, mockUser, "첫 번째 댓글");
            Comment comment2 = Comment.create(mockReview, mockUser, "두 번째 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of(comment1, comment2));
            given(userRepository.findAllById(anyCollection())).willReturn(List.of(mockUser));
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(2L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "DESC", null, null, 50);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).content()).isEqualTo("첫 번째 댓글");
            assertThat(result.content().get(0).userNickname()).isEqualTo("테스트유저");
            assertThat(result.content().get(1).content()).isEqualTo("두 번째 댓글");
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(2L);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("반환 건수가 limit과 같으면 hasNext가 true이고 nextCursor/nextAfter가 채워진다")
        void getComments_sizeEqualsLimit_hasNextTrue() {
            Instant createdAt = Instant.now().minusSeconds(5);
            // 비영속 엔티티는 @CreatedDate가 적용되지 않아 createdAt이 null이므로 mock 사용
            Comment comment = mock(Comment.class);
            given(comment.getUserId()).willReturn(USER_ID);
            given(comment.getCreatedAt()).willReturn(createdAt);
            given(comment.getUpdatedAt()).willReturn(createdAt);
            given(comment.getId()).willReturn(COMMENT_ID);
            given(comment.getReviewId()).willReturn(REVIEW_ID);
            given(comment.getContent()).willReturn("마지막 댓글");

            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(mockUser.getName()).willReturn("테스트유저");

            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of(comment));
            given(userRepository.findAllById(anyCollection())).willReturn(List.of(mockUser));
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(10L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "DESC", null, null, 1);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(createdAt.toString());
            assertThat(result.nextAfter()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("direction=DESC, cursor 있으면 해당 커서 이전 데이터를 반환한다")
        void getComments_desc_withCursor_returnsPaginatedResults() {
            Instant cursor = Instant.now().minusSeconds(1);
            User mockUser = testUser();
            Review mockReview = testReview();
            Comment comment = Comment.create(mockReview, mockUser, "커서 이전 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    eq(REVIEW_ID), eq(cursor), any(Pageable.class)))
                    .willReturn(List.of(comment));
            given(userRepository.findAllById(anyCollection())).willReturn(List.of(mockUser));
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(5L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "DESC", cursor, null, 50);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).content()).isEqualTo("커서 이전 댓글");
            assertThat(result.content().get(0).userNickname()).isEqualTo("테스트유저");
            assertThat(result.totalElements()).isEqualTo(5L);
        }

        @Test
        @DisplayName("direction=ASC, after 없으면 시간 오름차순 첫 페이지를 반환한다")
        void getComments_asc_noAfter_returnsFirstPage() {
            User mockUser = testUser();
            Review mockReview = testReview();
            Comment comment1 = Comment.create(mockReview, mockUser, "오래된 댓글");
            Comment comment2 = Comment.create(mockReview, mockUser, "최신 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtAsc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of(comment1, comment2));
            given(userRepository.findAllById(anyCollection())).willReturn(List.of(mockUser));
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(2L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "ASC", null, null, 50);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).content()).isEqualTo("오래된 댓글");
            assertThat(result.content().get(0).userNickname()).isEqualTo("테스트유저");
            assertThat(result.content().get(1).content()).isEqualTo("최신 댓글");
        }

        @Test
        @DisplayName("direction=ASC, after 있으면 해당 시간 이후 데이터를 반환한다")
        void getComments_asc_withAfter_returnsPaginatedResults() {
            Instant after = Instant.now().minusSeconds(1);
            User mockUser = testUser();
            Review mockReview = testReview();
            Comment comment = Comment.create(mockReview, mockUser, "after 이후 댓글");
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdAndCreatedAtAfterOrderByCreatedAtAsc(
                    eq(REVIEW_ID), eq(after), any(Pageable.class)))
                    .willReturn(List.of(comment));
            given(userRepository.findAllById(anyCollection())).willReturn(List.of(mockUser));
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(3L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "ASC", null, after, 50);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).content()).isEqualTo("after 이후 댓글");
            assertThat(result.content().get(0).userNickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("결과 없을 때 빈 content와 hasNext=false를 반환한다")
        void getComments_noResults_returnsEmptyContent() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(true);
            given(commentRepository.findByReviewIdOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
                    .willReturn(List.of());
            given(userRepository.findAllById(anyCollection())).willReturn(List.of());
            given(commentRepository.countByReviewId(REVIEW_ID)).willReturn(0L);

            CommentDto.CommentsResponse result = commentService.getComments(REVIEW_ID, "DESC", null, null, 50);

            assertThat(result.content()).isEmpty();
            assertThat(result.size()).isEqualTo(0);
            assertThat(result.totalElements()).isEqualTo(0L);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextAfter()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 reviewId로 조회하면 예외가 발생한다")
        void getComments_reviewNotFound_throwsException() {
            given(reviewRepository.existsById(REVIEW_ID)).willReturn(false);

            assertThatThrownBy(() -> commentService.getComments(REVIEW_ID, "DESC", null, null, 50))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 상세 조회
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 상세 조회")
    class GetComment {

        @Test
        @DisplayName("commentId로 댓글 상세 정보를 닉네임과 함께 조회한다")
        void getComment_success() {
            Review mockReview = mock(Review.class);
            given(mockReview.getId()).willReturn(REVIEW_ID);
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(mockUser.getName()).willReturn("테스트유저");

            Comment comment = Comment.create(mockReview, mockUser, "상세 조회할 댓글입니다.");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentDto.CommentResponse response = commentService.getComment(COMMENT_ID);

            assertThat(response.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.content()).isEqualTo("상세 조회할 댓글입니다.");
            assertThat(response.userNickname()).isEqualTo("테스트유저");
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