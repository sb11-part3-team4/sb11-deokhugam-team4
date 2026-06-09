package com.part3_team4.deokhoogam.domain.review;

import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("ReviewRepository 테스트")
class ReviewRepositoryTest extends RepositoryTestSupport {
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("userId와 bookId가 일치하는 리뷰가 존재하면 true를 반환한다")
    void existsByUserIdAndBookId_존재하면_true() {
        //given
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "좋은 책");
        reviewRepository.save(review);

        //when
        boolean result = reviewRepository.existsByUserIdAndBookId(userId, bookId);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("bookId가 다르면 false를 반환한다")
    void existsByUserIdAndBookId_bookId가_다르면_false() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "좋은 책");
        reviewRepository.save(review);

        boolean result = reviewRepository.existsByUserIdAndBookId(userId, UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("userId가 다르면 false를 반환한다")
    void existsByUserIdAndBookId_userId가_다르면_false() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Review review = Review.create(userId, bookId, 4, "좋은 책");
        reviewRepository.save(review);

        boolean result = reviewRepository.existsByUserIdAndBookId(UUID.randomUUID(), bookId);

        assertThat(result).isFalse();
    }

}

