package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);
    void deleteAllByReviewId(UUID reviewId);

}
