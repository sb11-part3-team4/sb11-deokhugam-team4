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
    //해당 기간에 작성된 리뷰와 연결된 인기 점수의 합산을 유저별로 구분
    List<Tuple> results = queryFactory
        .select(review.userId, review.count())
        .from(review)
        .where(review.createdAt.between(startDate, endDate))
        .groupBy(review.userId)
        .fetch();

    return results.stream()
        .filter(tuple -> tuple.get(review.userId) != null)
        .collect(Collectors.toMap(
            tuple -> tuple.get(review.userId),
            tuple -> {
              Long count = tuple.get(1, Long.class);
              return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
            },
            (existing, replacement) -> existing
        ));
  }

  @Override
  public Map<UUID, Long> getLikeCount(Instant startDate, Instant endDate) {
    //해당 기간에 유저가 직접 누른 좋아요 개수를 유저별로 구합니다
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
            tuple -> tuple.get(1, Long.class) != null ? tuple.get(1, Long.class) : 0L,
            (existing, replacement) -> existing
        ));
  }

  @Override
  public Map<UUID, Long> getCommentCount(Instant startDate, Instant endDate) {
    //해당 기간에 유저가 직접 작성한 댓글 개수를 유저별로 구합니다.
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
            tuple -> tuple.get(1, Long.class) != null ? tuple.get(1, Long.class) : 0L,
            (existing, replacement) -> existing
        ));
  }
}
