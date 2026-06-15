package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularReviewCalculator {

    private final ReviewRepository reviewRepository;
    private final PopularReviewRepository popularReviewRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void calculateAndSave(PeriodType period) {

        PopularReviewPeriod range = PopularReviewPeriod.of(period);

        List<Review> reviews = reviewRepository.findByCreatedAtBetween(range.getStart(),
            range.getEnd());

        List<Scored> scored = new ArrayList<>();
        for (Review review : reviews) {
            BigDecimal score = BigDecimal.valueOf(
                review.getLikeCount() * 1.0 + review.getCommentCount() * 0.5);
            scored.add(new Scored(review, score));
        }

        scored.sort(Comparator.comparing(Scored::score).reversed()
            .thenComparing(s -> s.review().getCreatedAt()));

        popularReviewRepository.deleteByPeriod(period.name());
        popularReviewRepository.flush();

        List<PopularReview> results = new ArrayList<>();
        int rank = 1;
        for (Scored s : scored) {
            results.add(PopularReview.create(
                s.review().getId(),
                period.name(),
                s.score(),
                rank++,
                LocalDate.now(ZoneId.of("Asia/Seoul"))
            ));
        }
        popularReviewRepository.saveAll(results);

        // 커밋 후 캐시 삭제
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            String pattern = "ranking:review:" + period + ":*";
                            ScanOptions options = ScanOptions.scanOptions().match(pattern)
                                .count(100).build();
                            List<String> keysToDelete = new ArrayList<>();
                            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                                cursor.forEachRemaining(keysToDelete::add);
                            }
                            if (!keysToDelete.isEmpty()) {
                                redisTemplate.delete(keysToDelete);
                            }
                        } catch (Exception e) {
                            log.warn("Redis 캐시 삭제 실패 (인기 리뷰): period={}", period, e);
                        }
                    }
                }
            );
        }
    }
    private record Scored(Review review, BigDecimal score) {}

}
