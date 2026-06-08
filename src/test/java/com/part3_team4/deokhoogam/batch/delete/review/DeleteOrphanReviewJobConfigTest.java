package com.part3_team4.deokhoogam.batch.delete.review;

import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class DeleteOrphanReviewJobConfigTest {

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
    private DeletedReviewRepository deletedReviewRepository;

    @Autowired
    @Qualifier("deleteOrphanReviewJob")
    private Job deleteOrphanReviewJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(deleteOrphanReviewJob);
        jobRepositoryTestUtils.removeJobExecutions();
        // H2 PostgreSQL 모드에서 uk_book_isbn 제약 조건명 충돌로 deleted_book 테이블이
        // Hibernate DDL 자동 생성에 실패할 수 있으므로 명시적으로 테이블을 보장한다.
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS deleted_book (" +
            "id UUID PRIMARY KEY, " +
            "title VARCHAR(255) NOT NULL, " +
            "author VARCHAR(100) NOT NULL, " +
            "description TEXT NOT NULL, " +
            "publisher VARCHAR(100) NOT NULL, " +
            "published_date DATE NOT NULL, " +
            "isbn TEXT, " +
            "thumbnail_url VARCHAR(512), " +
            "review_count INTEGER NOT NULL, " +
            "rating DECIMAL(3,2) NOT NULL, " +
            "deleted_at TIMESTAMPTZ NOT NULL, " +
            "created_at TIMESTAMPTZ NOT NULL, " +
            "updated_at TIMESTAMPTZ NOT NULL" +
            ")"
        );
        jdbcTemplate.execute("DELETE FROM deleted_comment");
        jdbcTemplate.execute("DELETE FROM comment");
        jdbcTemplate.execute("DELETE FROM deleted_review");
        jdbcTemplate.execute("DELETE FROM review");
        jdbcTemplate.execute("DELETE FROM deleted_book");
        jdbcTemplate.execute("DELETE FROM book");
        jdbcTemplate.execute("DELETE FROM deleted_user");
        jdbcTemplate.execute("DELETE FROM \"user\"");
    }

    private void insertDeletedReview(UUID id, UUID bookId, UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO deleted_review (id, user_id, book_id, rating, content, like_count, comment_count, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId, bookId, 5, "review content", 0, 0, Instant.now(), Instant.now(), Instant.now()
        );
    }

    private void insertBook(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO book (id, title, author, description, publisher, published_date, isbn, thumbnail_url, review_count, rating, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, "title", "author", "description", "publisher", LocalDate.now(), null, null, 0, new BigDecimal("0.00"), Instant.now(), Instant.now()
        );
    }

    private void insertDeletedBook(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO deleted_book (id, title, author, description, publisher, published_date, isbn, thumbnail_url, review_count, rating, deleted_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, "title", "author", "description", "publisher", LocalDate.now(), null, null, 0, new BigDecimal("0.00"), Instant.now(), Instant.now(), Instant.now()
        );
    }

    private void insertUser(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO \"user\" (id, email, nickname, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, id + "@test.com", "u_" + id.toString().substring(0, 8), "pw", Instant.now(), Instant.now()
        );
    }

    private void insertDeletedUser(UUID id) {
        jdbcTemplate.update(
            "INSERT INTO deleted_user (id, email, nickname, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, id + "@del.com", "d_" + id.toString().substring(0, 8), Instant.now(), Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("book이 book, deleted_book 어디에도 없으면 deleted_review가 삭제된다")
    void orphanByMissingBook_isDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID orphanId = UUID.randomUUID();
        insertDeletedReview(orphanId, UUID.randomUUID(), userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("user가 user, deleted_user 어디에도 없으면 deleted_review가 삭제된다")
    void orphanByMissingUser_isDeleted() throws Exception {
        UUID bookId = UUID.randomUUID();
        insertBook(bookId);
        UUID orphanId = UUID.randomUUID();
        insertDeletedReview(orphanId, bookId, UUID.randomUUID());

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(orphanId)).isEmpty();
    }

    @Test
    @DisplayName("book이 book 테이블에 존재하면 deleted_review가 삭제되지 않는다")
    void normalReview_withBookInBookTable_isNotDeleted() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertBook(bookId);
        insertUser(userId);
        UUID reviewId = UUID.randomUUID();
        insertDeletedReview(reviewId, bookId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(reviewId)).isPresent();
    }

    @Test
    @DisplayName("book이 deleted_book 테이블에 존재하면 deleted_review가 삭제되지 않는다")
    void normalReview_withBookInDeletedBookTable_isNotDeleted() throws Exception {
        UUID deletedBookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertDeletedBook(deletedBookId);
        insertUser(userId);
        UUID reviewId = UUID.randomUUID();
        insertDeletedReview(reviewId, deletedBookId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(reviewId)).isPresent();
    }

    @Test
    @DisplayName("user가 user 테이블에 존재하면 deleted_review가 삭제되지 않는다")
    void normalReview_withUserInUserTable_isNotDeleted() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertBook(bookId);
        insertUser(userId);
        UUID reviewId = UUID.randomUUID();
        insertDeletedReview(reviewId, bookId, userId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(reviewId)).isPresent();
    }

    @Test
    @DisplayName("user가 deleted_user 테이블에 존재하면 deleted_review가 삭제되지 않는다")
    void normalReview_withUserInDeletedUserTable_isNotDeleted() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID deletedUserId = UUID.randomUUID();
        insertBook(bookId);
        insertDeletedUser(deletedUserId);
        UUID reviewId = UUID.randomUUID();
        insertDeletedReview(reviewId, bookId, deletedUserId);

        JobExecution result = jobLauncherTestUtils.launchJob();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(deletedReviewRepository.findById(reviewId)).isPresent();
    }
}