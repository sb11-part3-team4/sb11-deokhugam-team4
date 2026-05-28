package com.part3_team4.deokhoogam.domain.comment;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class DeletedCommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DeletedCommentRepository deletedCommentRepository;

    @Autowired
    private TestEntityManager em;

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Comment persistedComment(String content) {
        Comment comment = Comment.create(REVIEW_ID, USER_ID, content);
        commentRepository.save(comment);
        em.flush();
        em.clear();
        return commentRepository.findById(comment.getId()).get();
    }

    private DeletedComment persistedDeletedComment(UUID reviewId, UUID userId, String content) {
        Comment comment = Comment.create(reviewId, userId, content);
        commentRepository.save(comment);
        em.flush();
        em.clear();
        Comment persisted = commentRepository.findById(comment.getId()).get();
        DeletedComment deletedComment = DeletedComment.from(persisted);
        commentRepository.delete(persisted);
        em.flush();
        deletedCommentRepository.save(deletedComment);
        em.flush();
        em.clear();
        return deletedComment;
    }

    @Test
    @DisplayName("DeletedComment를 저장하고 ID로 조회한다")
    void saveAndFindById() {
        Comment comment = persistedComment("삭제될 댓글입니다.");
        DeletedComment deletedComment = DeletedComment.from(comment);
        commentRepository.delete(comment);
        deletedCommentRepository.save(deletedComment);
        em.flush();
        em.clear();

        Optional<DeletedComment> result = deletedCommentRepository.findById(deletedComment.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("삭제될 댓글입니다.");
        assertThat(result.get().getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(result.get().getUserId()).isEqualTo(USER_ID);
        assertThat(result.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("DeletedComment의 id는 원본 Comment의 id와 동일하다")
    void deletedComment_id_equals_original_comment_id() {
        Comment comment = persistedComment("comment_id로 조회할 댓글입니다.");
        UUID commentId = comment.getId();
        DeletedComment deletedComment = DeletedComment.from(comment);
        commentRepository.delete(comment);
        deletedCommentRepository.save(deletedComment);
        em.flush();
        em.clear();

        Optional<DeletedComment> result = deletedCommentRepository.findById(commentId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(commentId);
    }

    @Test
    @DisplayName("DeletedComment를 물리 삭제하면 완전히 제거된다")
    void hardDelete_removesCompletely() {
        Comment comment = persistedComment("물리 삭제될 댓글입니다.");
        DeletedComment deletedComment = DeletedComment.from(comment);
        commentRepository.delete(comment);
        deletedCommentRepository.save(deletedComment);
        em.flush();
        em.clear();

        deletedCommentRepository.deleteById(deletedComment.getId());
        em.flush();
        em.clear();

        Optional<DeletedComment> result = deletedCommentRepository.findById(deletedComment.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("해당 reviewId의 논리 삭제된 댓글이 모두 물리 삭제된다")
    void deleteByReviewId_removesAllMatchingDeletedComments() {
        UUID otherReviewId = UUID.randomUUID();
        persistedDeletedComment(REVIEW_ID, USER_ID, "리뷰A 삭제 댓글 1");
        persistedDeletedComment(REVIEW_ID, USER_ID, "리뷰A 삭제 댓글 2");
        DeletedComment dc3 = persistedDeletedComment(otherReviewId, USER_ID, "리뷰B 삭제 댓글");

        deletedCommentRepository.deleteByReviewId(REVIEW_ID);
        em.flush();
        em.clear();

        assertThat(deletedCommentRepository.countByReviewId(REVIEW_ID)).isZero();
        assertThat(deletedCommentRepository.findById(dc3.getId())).isPresent();
    }

    @Test
    @DisplayName("해당 reviewId의 논리 삭제된 댓글 수를 정확히 반환한다")
    void countByReviewId_returnsAccurateCount() {
        UUID otherReviewId = UUID.randomUUID();
        persistedDeletedComment(REVIEW_ID, USER_ID, "리뷰A 삭제 댓글 1");
        persistedDeletedComment(REVIEW_ID, USER_ID, "리뷰A 삭제 댓글 2");
        persistedDeletedComment(otherReviewId, USER_ID, "리뷰B 삭제 댓글");

        assertThat(deletedCommentRepository.countByReviewId(REVIEW_ID)).isEqualTo(2);
        assertThat(deletedCommentRepository.countByReviewId(otherReviewId)).isEqualTo(1);
    }

    @Test
    @DisplayName("해당 userId의 논리 삭제된 댓글 수를 정확히 반환한다")
    void countByUserId_returnsAccurateCount() {
        UUID otherUserId = UUID.randomUUID();
        persistedDeletedComment(REVIEW_ID, USER_ID, "유저A 삭제 댓글 1");
        persistedDeletedComment(REVIEW_ID, USER_ID, "유저A 삭제 댓글 2");
        persistedDeletedComment(REVIEW_ID, otherUserId, "유저B 삭제 댓글");

        assertThat(deletedCommentRepository.countByUserId(USER_ID)).isEqualTo(2);
        assertThat(deletedCommentRepository.countByUserId(otherUserId)).isEqualTo(1);
    }
}