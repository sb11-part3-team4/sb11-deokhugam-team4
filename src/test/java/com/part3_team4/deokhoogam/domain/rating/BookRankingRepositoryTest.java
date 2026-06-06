package com.part3_team4.deokhoogam.domain.rating;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.ranking.dto.BookScoreProjection;
import com.part3_team4.deokhoogam.domain.ranking.repository.BookRankingRepository;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@ActiveProfiles("test")
@DisplayName("BookRankingRepository 테스트")

class BookRankingRepositoryTest {

  @Autowired
  private BookRankingRepository bookRankingRepository;
  @Autowired
  private EntityManager em;

  // 기준 시각: 오늘 0시 / 어제 0시
  private static final Instant TODAY = Instant.now().truncatedTo(ChronoUnit.DAYS);
  private static final Instant YESTERDAY = TODAY.minus(1, ChronoUnit.DAYS);

  //리뷰 추가 테스트
  private void saveReview(UUID bookId, int rating, Instant createdAt) {
    Review review = Review.create(UUID.randomUUID(), bookId, rating, "내용");
    em.persist(review);
    em.flush();
    // auditing 이 박은 created_at 을 원하는 시각으로 덮어씀
    em.createQuery("update Review r set r.createdAt = :ts where r.id = :id")
        .setParameter("ts", createdAt)
        .setParameter("id", review.getId())
        .executeUpdate();
    em.clear();
  }

  //삭제된 리뷰 추가
  private void saveDeletedReview(UUID bookId, int rating, Instant createdAt) {
    em.createNativeQuery("""
        INSERT INTO deleted_review
          (id, user_id, book_id, rating, content, like_count, comment_count,
           created_at, updated_at, deleted_at)
        VALUES (?, ?, ?, ?, '내용', 0, 0, ?, ?, ?)
        """)
        .setParameter(1, UUID.randomUUID())
        .setParameter(2, UUID.randomUUID())
        .setParameter(3, bookId)
        .setParameter(4, rating)
        .setParameter(5, createdAt)
        .setParameter(6, createdAt)
        .setParameter(7, Instant.now())
        .executeUpdate();
  }

  @Test
  @DisplayName("같은 책 어제 리뷰 2개가 있으면 점수를 계산한다.")
  void aggregate_basic() {
    UUID bookId = UUID.randomUUID();
    //평균 평점 4.5
    saveReview(bookId, 4, YESTERDAY.plus(1, ChronoUnit.HOURS));
    saveReview(bookId, 5, YESTERDAY.plus(2, ChronoUnit.HOURS));

    List<BookScoreProjection> result = bookRankingRepository.aggregateScores(YESTERDAY, TODAY);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getReviewCount()).isEqualTo(2);
    assertThat(result.get(0).getAvgRating()).isEqualByComparingTo("4.5");
  }

  @Test
  @DisplayName("review 1개 + deleted_review 1개 → 개수 2가 잘 나온다)")
  void aggregate_includesDeleted() {
    UUID bookId = UUID.randomUUID();
    saveReview(bookId, 4, YESTERDAY.plus(1, ChronoUnit.HOURS));
    saveDeletedReview(bookId, 2, YESTERDAY.plus(2, ChronoUnit.HOURS));

    List<BookScoreProjection> result = bookRankingRepository.aggregateScores(YESTERDAY, TODAY);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getReviewCount()).isEqualTo(2);
    assertThat(result.get(0).getAvgRating()).isEqualByComparingTo("3.0");
  }

  @Test
  @DisplayName("기간 밖(일주일 전) 리뷰는 제외된다")
  void aggregate_excludesOutOfRange() {
    UUID bookId = UUID.randomUUID();
    saveReview(bookId, 5, YESTERDAY.plus(1, ChronoUnit.HOURS));      // 어제 → 포함
    saveReview(bookId, 1, YESTERDAY.minus(7, ChronoUnit.DAYS));     // 일주일 전 → 제외

    List<BookScoreProjection> result = bookRankingRepository.aggregateScores(YESTERDAY, TODAY);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getReviewCount()).isEqualTo(1);        // 어제 것만
    assertThat(result.get(0).getAvgRating()).isEqualByComparingTo("5.0");
  }
}