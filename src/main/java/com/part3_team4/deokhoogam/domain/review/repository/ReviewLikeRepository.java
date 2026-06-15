package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ReviewLike rl WHERE rl.reviewId = :reviewId")
    void deleteAllByReviewId(@Param("reviewId") UUID reviewId);

    // SELECT 후 DELETE 하는  방식 대신, 원자적으로 삭제하도록 쿼리 명시
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ReviewLike rl WHERE rl.reviewId = :reviewId AND rl.userId = :userId")
    void deleteByReviewIdAndUserId(@Param("reviewId") UUID reviewId, @Param("userId") UUID userId);

    @Query("SELECT rl.reviewId FROM ReviewLike rl WHERE rl.userId = :userId AND rl.reviewId IN :reviewIds")
    List<UUID> findLikedReviewIds(@Param("userId") UUID userId, @Param("reviewIds") List<UUID> reviewIds);
}