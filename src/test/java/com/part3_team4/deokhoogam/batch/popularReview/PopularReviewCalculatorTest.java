package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("점수가 높은 리뷰가 낮은 순위(rank=1)를 가진다")
    void calculateAndSave_scoresAndRanksCorrectly() {
        Review review1 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "리뷰1");
        ReflectionTestUtils.setField(review1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(review1, "createdAt", Instant.now());
        ReflectionTestUtils.setField(review1, "likeCount", 2);
        ReflectionTestUtils.setField(review1, "commentCount", 2);

        Review review2 = Review.create(UUID.randomUUID(), UUID.randomUUID(), 3, "리뷰2");
        ReflectionTestUtils.setField(review2, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(review2, "createdAt", Instant.now());
        ReflectionTestUtils.setField(review2, "likeCount", 1);
        ReflectionTestUtils.setField(review2, "commentCount", 0);

        given(reviewRepository.findByCreatedAtBetween(any(), any())).willReturn(List.of(review2, review1));

        popularReviewCalculator.calculateAndSave(PeriodType.DAILY);

        ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
        then(popularReviewRepository).should().saveAll(captor.capture());

        List<PopularReview> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getRank()).isEqualTo(1);
        assertThat(saved.get(0).getScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(saved.get(1).getRank()).isEqualTo(2);
        assertThat(saved.get(1).getScore()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("리뷰가 없으면 빈 리스트를 저장한다")
    void calculateAndSave_emptyReviews_savesEmptyList() {
        given(reviewRepository.findByCreatedAtBetween(any(),any())).willReturn(List.of());

        popularReviewCalculator.calculateAndSave(PeriodType.DAILY);

        then(popularReviewRepository).should().saveAll(eq(List.of()));
    }
}
