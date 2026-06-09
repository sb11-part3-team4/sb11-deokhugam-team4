package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PowerUserRankingService {

  private final PowerUserRankingRepository powerUserRankingRepository;

  public record UserScore(UUID userId, double score) {

  }

  public List<UserScore> calculateScores(Map<UUID, BigDecimal> reviewScores,
      Map<UUID, Long> likeCounts,
      Map<UUID, Long> commentCounts) {
    Set<UUID> activeUserIds = new HashSet<>();
    activeUserIds.addAll(reviewScores.keySet());
    activeUserIds.addAll(likeCounts.keySet());
    activeUserIds.addAll(commentCounts.keySet());

    List<UserScore> userScores = new ArrayList<>();

    for (UUID userId : activeUserIds) {
      double reviewScore = reviewScores.getOrDefault(userId, BigDecimal.ZERO).doubleValue();
      long likeCount = likeCounts.getOrDefault(userId, 0L);
      long commentCount = commentCounts.getOrDefault(userId, 0L);

      // (리뷰 * 0.5) + (좋아요 * 0.2) + (댓글 * 0.3)
      double totalScore = (reviewScore * 0.5) + (likeCount * 0.2) + (commentCount * 0.3);

      if (totalScore > 0) {
        userScores.add(new UserScore(userId, totalScore));
      }
    }
    return userScores;
  }

  @Transactional
  public void calculateAndSaveDailyRanking() {
    Instant now = Instant.now();
    Instant startDate = now.minus(1, ChronoUnit.DAYS);

    // 기존 당일 데이터 제거
    powerUserRankingRepository.deleteByPeriod(PowerUserPeriod.DAILY);

    // 원시 데이터 집계
    Map<UUID, BigDecimal> reviewScores = powerUserRankingRepository.getReviewPopularScoreSum(
        startDate, now);
    Map<UUID, Long> likeCounts = powerUserRankingRepository.getLikeCount(startDate, now);
    Map<UUID, Long> commentCounts = powerUserRankingRepository.getCommentCount(startDate, now);

    // 점수 계산 로직 호출
    List<UserScore> userScores = calculateScores(reviewScores, likeCounts, commentCounts);

    if (userScores.isEmpty()) {
      return;
    }

    // 랭킹 정렬 및 부여 로직 호출
    List<PowerUserRanking> rankings = assignRankings(userScores, PowerUserPeriod.DAILY);

    powerUserRankingRepository.saveAll(rankings);
  }

  public List<PowerUserRanking> assignRankings(List<UserScore> userScores, PowerUserPeriod period) {
    // 점수 기준 내림차순 정렬
    List<UserScore> sortedScores = new ArrayList<>(userScores);
    sortedScores.sort((a, b) -> Double.compare(b.score(), a.score()));

    List<PowerUserRanking> rankings = new ArrayList<>();
    for (int i = 0; i < sortedScores.size(); i++) {
      rankings.add(PowerUserRanking.builder()
          .userId(sortedScores.get(i).userId())
          .period(period)
          .score(sortedScores.get(i).score())
          .ranking(i + 1)
          .build());
    }
    return rankings;
  }
}
