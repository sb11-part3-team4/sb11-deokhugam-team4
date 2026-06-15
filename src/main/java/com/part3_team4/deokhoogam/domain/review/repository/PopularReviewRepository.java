package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {
    @Query(value = """
        SELECT pr.* FROM popular_review pr
        INNER JOIN review r ON pr.review_id = r.id
        INNER JOIN book b ON r.book_id = b.id
        INNER JOIN "user" u ON r.user_id = u.id
        WHERE pr.period = :period
        ORDER BY pr.rank ASC
        """, nativeQuery = true)
    List<PopularReview> findByPeriodOrderByRankAsc(@Param("period") String period, Pageable pageable);

    // 리뷰 ID로 인기 리뷰 기록 삭제
    void deleteAllByReviewId(UUID reviewId);
    @Query(value = """
        SELECT pr.* FROM popular_review pr
        INNER JOIN review r ON pr.review_id = r.id
        INNER JOIN book b ON r.book_id = b.id
        INNER JOIN "user" u ON r.user_id = u.id
        WHERE pr.period = :period
        ORDER BY pr.rank DESC 
        """, nativeQuery = true)
    List<PopularReview> findByPeriodOrderByRankDesc(@Param("period") String period, Pageable pageable);

    @Query(value = """
            SELECT pr.* FROM popular_review pr
            INNER JOIN review r ON pr.review_id = r.id
            INNER JOIN book b ON r.book_id = b.id
            INNER JOIN "user" u ON r.user_id = u.id
            WHERE pr.period = :period AND pr.rank > :rank
            """, nativeQuery = true)
    List<PopularReview> findByPeriodAndRankGreaterThan(@Param("period") String period, @Param("rank") int rank, Pageable pageable);

    @Query(value = """
            SELECT pr.* FROM popular_review pr
            INNER JOIN review r ON pr.review_id = r.id
            INNER JOIN book b ON r.book_id = b.id
            INNER JOIN "user" u ON r.user_id = u.id
            WHERE pr.period = :period AND pr.rank < :rank
            """, nativeQuery = true)
    List<PopularReview> findByPeriodAndRankLessThan(@Param("period") String period, @Param("rank") int rank, Pageable pageable);

    void deleteByPeriod(String period);
}
