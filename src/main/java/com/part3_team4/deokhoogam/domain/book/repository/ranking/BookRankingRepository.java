package com.part3_team4.deokhoogam.domain.book.repository.ranking;

import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookScoreProjection;
import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRankingRepository extends JpaRepository<BookRanking, UUID>,
    BookRankingRepositoryCustom {

  @Query(value = """
        SELECT CAST(book_id AS UUID) AS bookId,
               COUNT(*) AS reviewCount,
               AVG(CAST(rating AS DECIMAL(3,2))) AS avgRating
        FROM (
            SELECT book_id, rating, created_at FROM review
            UNION ALL
            SELECT book_id, rating, created_at FROM deleted_review
        ) AS all_reviews
        WHERE created_at >= :start AND created_at < :end
        GROUP BY book_id
        """, nativeQuery = true)
  List<BookScoreProjection> aggregateScores(@Param("start") Instant start,
      @Param("end") Instant end);

  @Query(value = """
        SELECT CAST(book_id AS UUID) AS bookId,
               COUNT(*) AS reviewCount,
               AVG(CAST(rating AS DECIMAL(3,2))) AS avgRating
        FROM (
            SELECT book_id, rating FROM review
            UNION ALL
            SELECT book_id, rating FROM deleted_review
        ) AS all_reviews
        GROUP BY book_id
        """, nativeQuery = true)
  List<BookScoreProjection> aggregateAllTimeScores();

  @Query(value = """
    SELECT book_id
    FROM review
    LIMIT 1
    """, nativeQuery = true)
  Object findBookIdRaw();

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  void deleteByPeriod(PeriodType period);
}