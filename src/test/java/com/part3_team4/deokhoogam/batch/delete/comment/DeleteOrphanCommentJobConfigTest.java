package com.part3_team4.deokhoogam.batch.delete.comment;

import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class DeleteOrphanCommentJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DeletedCommentRepository deletedCommentRepository;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.execute("DELETE FROM deleted_comment");
        jdbcTemplate.execute("DELETE FROM deleted_review");
        jdbcTemplate.execute("DELETE FROM deleted_user");
        jdbcTemplate.execute("DELETE FROM review");
        jdbcTemplate.execute("DELETE FROM \"user\"");
    }

    private void insertDeletedComment(UUID id, UUID reviewId, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO deleted_comment (id, review_id, user_id, content, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, reviewId, userId, "content", Instant.now(), Instant.now(), Instant.now()
        );
    }

    private void insertUser(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO \"user\" (id, email, nickname, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, id + "@test.com", "u_" + id.toString().substring(0, 8), "pw", Instant.now(), Instant.now()
        );
    }

    private void insertReview(UUID id, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO review (id, user_id, book_id, rating, content, like_count, comment_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId, UUID.randomUUID(), 5, "review", 0, 0, Instant.now(), Instant.now()
        );
    }

    private void insertDeletedReview(UUID id, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO deleted_review (id, user_id, book_id, rating, content, like_count, comment_count, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId, UUID.randomUUID(), 5, "deleted review", 0, 0, Instant.now(), Instant.now(), Instant.now()
        );
    }

    private void insertDeletedUser(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO deleted_user (id, email, nickname, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, id + "@del.com", "d_" + id.toString().substring(0, 8), Instant.now(), Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("review가 review, deleted_review 어디에도 없으면 deleted_comment가 삭제된다")
    void orphanByMissingReview_isDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID orphanId = UUID.randomUUID();
        insertDeletedComment(orphanId, UUID.randomUUID(), userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCommentRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("user가 user, deleted_user 어디에도 없으면 deleted_comment가 삭제된다")
    void orphanByMissingUser_isDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        insertReview(reviewId, userId);
        UUID orphanId = UUID.randomUUID();
        insertDeletedComment(orphanId, reviewId, UUID.randomUUID());

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCommentRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("review와 user가 모두 존재하면 deleted_comment가 삭제되지 않는다")
    void normalComment_withValidReviewAndUser_isNotDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        insertUser(userId);
        insertReview(reviewId, userId);
        UUID commentId = UUID.randomUUID();
        insertDeletedComment(commentId, reviewId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCommentRepository.findById(commentId)).isPresent();
    }

    @Test
    @DisplayName("review가 deleted_review에 존재하면 deleted_comment가 삭제되지 않는다")
    void normalComment_withDeletedReview_isNotDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deletedReviewId = UUID.randomUUID();
        insertUser(userId);
        insertDeletedReview(deletedReviewId, userId);
        UUID commentId = UUID.randomUUID();
        insertDeletedComment(commentId, deletedReviewId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCommentRepository.findById(commentId)).isPresent();
    }

    @Test
    @DisplayName("user가 deleted_user에 존재하면 deleted_comment가 삭제되지 않는다")
    void normalComment_withDeletedUser_isNotDeleted() throws Exception {
        UUID deletedUserId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        insertDeletedUser(deletedUserId);
        insertReview(reviewId, deletedUserId);
        UUID commentId = UUID.randomUUID();
        insertDeletedComment(commentId, reviewId, deletedUserId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedCommentRepository.findById(commentId)).isPresent();
    }
}