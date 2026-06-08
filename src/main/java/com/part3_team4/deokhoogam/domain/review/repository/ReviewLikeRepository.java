package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);
    void deleteAllByReviewId(UUID reviewId);
    void deleteByReviewIdAndUserId(UUID reviewId, UUID userId);

    @Query("SELECT rl.reviewId FROM ReviewLike rl WHERE rl.userId = :userId AND rl.reviewId IN :reviewIds")
    List<UUID> findLikedReviewIds(@Param("userId") UUID userId, @Param("reviewIds")
                                  List<UUID> reviewIds);
}
