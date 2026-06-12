package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class PopularReviewCalculatorTest {

    @InjectMocks
    PopularReviewCalculator popularReviewCalculator;

    @Mock
    ReviewRepository reviewRepository;

    @Mock
    PopularReviewRepository popularReviewRepository;

    @Test
    @DisplayName("calculateAndSave 호출 시 기존 데이터 삭제 후 새로 저장한다")
    void calculateAndSave_deletesAndSaves() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "좋은 책");
        ReflectionTestUtils.setField(review, "id", reviewId);
        ReflectionTestUtils.setField(review, "createdAt", Instant.now());

        given(reviewRepository.findByCreatedAtBetween(any(), any())).willReturn(List.of(review));

        popularReviewCalculator.calculateAndSave(PeriodType.DAILY);

        then(popularReviewRepository).should().deleteByPeriod("DAILY");
        then(popularReviewRepository).should().flush();
        then(popularReviewRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("리뷰가 없으면 빈 리스트를 저장한다")
    void calculateAndSave_emptyReviews_savesEmptyList() {
        given(reviewRepository.findByCreatedAtBetween(any(),any())).willReturn(List.of());

        popularReviewCalculator.calculateAndSave(PeriodType.DAILY);

        then(popularReviewRepository).should().saveAll(eq(List.of()));
    }
}
