package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewListRequest;
import com.part3_team4.deokhoogam.domain.review.entity.Review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID>{

    List<Review> findReviews(ReviewListRequest request);
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);

}
