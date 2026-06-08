package com.part3_team4.deokhoogam.domain.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.ranking.dto.BookScoreProjection;
import com.part3_team4.deokhoogam.domain.ranking.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.ranking.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.ranking.repository.BookRankingRepository;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
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



  @Nested
  @DisplayName("커서 페이지네이션 에서")

  class TestCursorPagination {

    private void saveRanking(PeriodType period, int ranking) {
      BookRanking r = BookRanking.builder()
          .bookId(UUID.randomUUID())
          .period(period)
          .score(new BigDecimal("1.0"))
          .ranking(ranking)
          .reviewCount(0)
          .rating(new BigDecimal("0.00"))
          .build();
      em.persist(r);
      em.flush();
    }

    @Test
    @DisplayName("period로 필터된다 (DAILY만 조회)")
    void filterByPeriod() {
      saveRanking(PeriodType.DAILY, 1);
      saveRanking(PeriodType.DAILY, 2);
      saveRanking(PeriodType.WEEKLY, 1);

      Slice<BookRanking> result = bookRankingRepository.getRankings(PeriodType.DAILY, Direction.ASC, null, 10);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent())
          .allMatch(r -> r.getPeriod() == PeriodType.DAILY);
    }

    @Test
    @DisplayName("ranking 오름차순으로 정렬된다")
    void orderByRanking() {
      saveRanking(PeriodType.DAILY, 3);
      saveRanking(PeriodType.DAILY, 1);
      saveRanking(PeriodType.DAILY, 2);

      Slice<BookRanking> result = bookRankingRepository.getRankings(PeriodType.DAILY, Direction.ASC,null, 10);

      assertThat(result.getContent())
          .extracting(BookRanking::getRanking)
          .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("커서 다음 페이지가 이어지고 hasNext가 맞다")
    void cursorPaging() {
      for (int i = 1; i <= 5; i++) {
        saveRanking(PeriodType.DAILY, i);
      }

      // 첫 페이지: ranking 1,2,3 / hasNext=true
      Slice<BookRanking> page1 = bookRankingRepository.getRankings(PeriodType.DAILY, Direction.ASC,null, 3);
      assertThat(page1.getContent()).extracting(BookRanking::getRanking).containsExactly(1, 2, 3);
      assertThat(page1.hasNext()).isTrue();

      // 다음 페이지: ranking 3 다음부터 → 4,5 / hasNext=false
      Slice<BookRanking> page2 = bookRankingRepository.getRankings(PeriodType.DAILY, Direction.ASC, 3, 3);
      assertThat(page2.getContent()).extracting(BookRanking::getRanking).containsExactly(4, 5);
      assertThat(page2.hasNext()).isFalse();
    }


  }












}