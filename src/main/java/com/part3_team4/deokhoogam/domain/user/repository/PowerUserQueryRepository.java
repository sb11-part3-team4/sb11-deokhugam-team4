package com.part3_team4.deokhoogam.domain.user.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface PowerUserQueryRepository {
  //기간 내 유저별 작성한 리뷰의 인기 점수 총합
  Map<UUID, BigDecimal> getReviewPopularScoreSum(Instant startDate, Instant endDate);

  //기간 내 유저별 직접 누른 좋아요 개수
  Map<UUID, Long> getLikeCount(Instant startDate, Instant endDate);

  //기간 내 유저별 작성한 댓글 개수
  Map<UUID, Long> getCommentCount(Instant startDate, Instant endDate);
}
