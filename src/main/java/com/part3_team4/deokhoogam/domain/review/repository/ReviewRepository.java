package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface ReviewRepository extends JpaRepository<Review, UUID>{
    @Query("""
        SELECT r FROM Review r
        WHERE (:userId IS NULL OR r.userId = :userId)
        AND (:bookId IS NULL OR r.bookId = :bookId)
        AND (:keyword IS NULL OR r.content LIKE CONCAT('%', :keyword, '%'))
        """)
    List<Review> findReviews(
            @Param("userId") UUID userId,
            @Param("bookId") UUID bookId,
            @Param("keyword") String keyword,
            Pageable pageable
            );
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);

}
