package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedReviewRepository extends JpaRepository<DeletedReview, UUID> {

}
