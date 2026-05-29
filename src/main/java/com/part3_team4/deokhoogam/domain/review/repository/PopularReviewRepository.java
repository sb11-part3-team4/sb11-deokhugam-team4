package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {

}
