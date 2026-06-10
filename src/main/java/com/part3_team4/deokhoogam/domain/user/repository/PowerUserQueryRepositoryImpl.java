package com.part3_team4.deokhoogam.domain.user.repository;

import static com.part3_team4.deokhoogam.domain.comment.entity.QComment.comment;
import static com.part3_team4.deokhoogam.domain.review.entity.QReview.review;
import static com.part3_team4.deokhoogam.domain.review.entity.QReviewLike.reviewLike;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PowerUserQueryRepositoryImpl implements PowerUserQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Map<UUID, BigDecimal> getReviewPopularScoreSum(Instant startDate, Instant endDate) {
    List<Tuple> results = queryFactory
        .select(review.userId, review.likeCount.sum())
        .from(review)
        .where(review.createdAt.between(startDate, endDate))
        .groupBy(review.userId)
        .fetch();

    return results.stream()
        .filter(tuple -> tuple.get(review.userId) != null)
        .collect(Collectors.toMap(
            tuple -> tuple.get(review.userId),
            tuple -> {
              Number sum = tuple.get(1, Number.class);
              return sum != null ? BigDecimal.valueOf(sum.longValue()) : BigDecimal.ZERO;
            },
            (existing, replacement) -> existing
        ));
  }

  @Override
  public Map<UUID, Long> getLikeCount(Instant startDate, Instant endDate) {
    List<Tuple> results = queryFactory
        .select(reviewLike.userId, reviewLike.count())
        .from(reviewLike)
        .where(reviewLike.createdAt.between(startDate, endDate))
        .groupBy(reviewLike.userId)
        .fetch();

    return results.stream()
        .filter(tuple -> tuple.get(reviewLike.userId) != null)
        .collect(Collectors.toMap(
            tuple -> tuple.get(reviewLike.userId),
            tuple -> {
              Number count = tuple.get(1, Number.class);
              return count != null ? count.longValue() : 0L;
            },
            (existing, replacement) -> existing
        ));
  }

  @Override
  public Map<UUID, Long> getCommentCount(Instant startDate, Instant endDate) {
    List<Tuple> results = queryFactory
        .select(comment.user.id, comment.count())
        .from(comment)
        .where(comment.createdAt.between(startDate, endDate))
        .groupBy(comment.user.id)
        .fetch();

    return results.stream()
        .filter(tuple -> tuple.get(comment.user.id) != null)
        .collect(Collectors.toMap(
            tuple -> tuple.get(comment.user.id),
            tuple -> {
              Number count = tuple.get(1, Number.class);
              return count != null ? count.longValue() : 0L;
            },
            (existing, replacement) -> existing
        ));
  }
}
