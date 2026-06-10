package com.part3_team4.deokhoogam.batch.delete.notification;

import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class DeleteOrphanNotificationJobConfigTest {

    @MockBean
    @SuppressWarnings("unused")
    private S3Template s3Template;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    @Qualifier("deleteOrphanNotificationJob")
    private Job deleteOrphanNotificationJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(deleteOrphanNotificationJob);
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM review");
        jdbcTemplate.execute("DELETE FROM \"user\"");
    }

    private void insertNotification(UUID id, UUID reviewId, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO notification (id, review_id, user_id, review_content, message, confirmed, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            id, reviewId, userId, "review content", "message", false, Instant.now(), Instant.now()
        );
    }

    private void insertReview(UUID id, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO review (id, user_id, book_id, rating, content, like_count, comment_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId, UUID.randomUUID(), 5, "review", 0, 0, Instant.now(), Instant.now()
        );
    }

    private void insertUser(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO \"user\" (id, email, nickname, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, id + "@test.com", "u_" + id.toString().substring(0, 8), "pw", Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("review가 review 테이블에 없으면 notification이 삭제된다")
    void orphanByMissingReview_isDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID orphanId = UUID.randomUUID();
        insertNotification(orphanId, UUID.randomUUID(), userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(notificationRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("user가 user 테이블에 없으면 notification이 삭제된다")
    void orphanByMissingUser_isDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        insertUser(userId);
        insertReview(reviewId, userId);
        UUID orphanId = UUID.randomUUID();
        insertNotification(orphanId, reviewId, UUID.randomUUID());

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(notificationRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("review와 user가 모두 존재하면 notification이 삭제되지 않는다")
    void normalNotification_withValidReviewAndUser_isNotDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        insertUser(userId);
        insertReview(reviewId, userId);
        UUID notificationId = UUID.randomUUID();
        insertNotification(notificationId, reviewId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(notificationRepository.findById(notificationId)).isPresent();
    }

    @Test
    @DisplayName("CHUNK_SIZE(100) 초과 고아 notification도 모두 삭제된다")
    void orphanNotifications_exceedingChunkSize_areAllDeleted() throws Exception {
        for (int i = 0; i < 150; i++) {
            insertNotification(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(notificationRepository.count()).isZero();
    }
}