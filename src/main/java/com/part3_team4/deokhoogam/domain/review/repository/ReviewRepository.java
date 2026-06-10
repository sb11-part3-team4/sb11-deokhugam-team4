package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ReviewRepository extends JpaRepository<Review, UUID>{
    @Query("""
        SELECT r FROM Review r
        WHERE (:userId IS NULL OR r.userId = :userId)
        AND (:bookId IS NULL OR r.bookId = :bookId)
        AND (:keyword IS NULL OR r.content LIKE CONCAT('%', CAST(:keyword AS String), '%'))
        """)
    List<Review> findReviews(
            @Param("userId") UUID userId,
            @Param("bookId") UUID bookId,
            @Param("keyword") String keyword,
            Pageable pageable
            );
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);


    @Query("""
        SELECT new com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked(
        r, CASE WHEN rl.id IS NOT NULL THEN true ELSE false END
        )
        FROM Review r
        LEFT JOIN ReviewLike rl ON rl.reviewId = r.id AND rl.userId = :userId
        WHERE r.id = :reviewId
""")
    Optional<ReviewWithLiked> findByIdWithLiked(
            @Param("reviewId") UUID reviewId,
            @Param("userId") UUID userId
    );

}
