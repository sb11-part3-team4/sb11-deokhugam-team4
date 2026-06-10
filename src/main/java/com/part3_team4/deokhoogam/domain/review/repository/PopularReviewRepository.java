package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {
    List<PopularReview> findByPeriod(String period, Pageable pageable);
    List<PopularReview> findByPeriodAndRankGreaterThan(String period, int rank, Pageable pageable);
    List<PopularReview> findByPeriodAndRankLessThan(String period, int rank, Pageable pageable);

}
