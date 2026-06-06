package com.part3_team4.deokhoogam.domain.ranking.repository;

import com.part3_team4.deokhoogam.domain.ranking.dto.BookScoreProjection;
import com.part3_team4.deokhoogam.domain.ranking.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.ranking.entity.PeriodType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRankingRepository extends JpaRepository<BookRanking, UUID> {

  @Query(value = """
        SELECT CAST(book_id AS UUID) AS bookId,
               COUNT(*) AS reviewCount,
               AVG(CAST(rating AS DECIMAL(10,4))) AS avgRating
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
               AVG(CAST(rating AS DECIMAL(10,4))) AS avgRating
        FROM (
            SELECT book_id, rating FROM review
            UNION ALL
            SELECT book_id, rating FROM deleted_review
        ) AS all_reviews
        GROUP BY book_id
        """, nativeQuery = true)
  List<BookScoreProjection> aggregateAllTimeScores();

  void deleteByPeriod(PeriodType period);
}