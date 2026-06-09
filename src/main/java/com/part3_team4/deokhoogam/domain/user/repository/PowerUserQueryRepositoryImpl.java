package com.part3_team4.deokhoogam.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.part3_team4.deokhoogam.domain.comment.entity.QComment.comment;
import static com.part3_team4.deokhoogam.domain.review.entity.QPopularReview.popularReview;
import static com.part3_team4.deokhoogam.domain.review.entity.QReview.review;
import static com.part3_team4.deokhoogam.domain.review.entity.QReviewLike.reviewLike;
import static com.querydsl.core.group.GroupBy.groupBy;

@Repository
@RequiredArgsConstructor
public class PowerUserQueryRepositoryImpl implements PowerUserQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Map<UUID, BigDecimal> getReviewPopularScoreSum(Instant startDate, Instant endDate) {
    //해당 기간에 작성된 리뷰와 연결된 인기 점수의 합산을 유저별로 구분
    return queryFactory
        .from(review)
        .leftJoin(popularReview).on(review.id.eq(popularReview.reviewId))
        .where(
            review.createdAt.between(startDate, endDate),
            popularReview.score.isNotNull()
        )
        .groupBy(review.userId)
        .transform(groupBy(review.userId).as(popularReview.score.sum()));
  }

  @Override
  public Map<UUID, Long> getLikeCount(Instant startDate, Instant endDate) {
    //해당 기간에 유저가 직접 누른 좋아요 개수를 유저별로 구합니다
    return queryFactory
        .from(reviewLike)
        .where(reviewLike.createdAt.between(startDate, endDate))
        .groupBy(reviewLike.userId)
        .transform(groupBy(reviewLike.userId).as(reviewLike.count()));
  }

  @Override
  public Map<UUID, Long> getCommentCount(Instant startDate, Instant endDate) {
    //해당 기간에 유저가 직접 작성한 댓글 개수를 유저별로 구합니다.
    return queryFactory
        .from(comment)
        .where(comment.createdAt.between(startDate, endDate))
        .groupBy(comment.user.id)
        .transform(groupBy(comment.user.id).as(comment.count()));
  }
}
