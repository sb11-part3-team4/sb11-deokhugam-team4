package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PowerUserRankingService {

  private final UserService userService;
  private final PowerUserRankingRepository powerUserRankingRepository;

  public record UserScore(UUID userId, BigDecimal score) {}

  public List<PowerUserRankingResponseDto> getRankingWithNickname(PowerUserPeriod period) {
    List<PowerUserRanking> rankings = powerUserRankingRepository.findByPeriodOrderByRankingAsc(period);

    return rankings.stream().map(ranking -> {
      String nickname = "알수없음"; // 기본값

      try {
        UserResponse user = userService.getUser(ranking.getUserId());
        if (user != null) nickname = user.nickname();
      } catch (UserNotFoundException e) {
        // 의도적인 빈 블록:
        // 물리 삭제된 유저의 랭킹 데이터가 남아있을 때 서버 에러 방지를 위해 예외만 잡고 기본값("알수없음") 유지.
      }

      // 1.4 -> 1.0, 0.6 -> 0.0 으로 변환되어 프론트에서 1점, 0점으로 표시됨
      double flooredScore = Math.floor(ranking.getScore().doubleValue());

      return new PowerUserRankingResponseDto(
          ranking.getUserId(), nickname, ranking.getRanking(), flooredScore
      );
    }).collect(Collectors.toList());
  }

  @Transactional
  public void calculateAndSaveAllRankings() {
    Instant now = Instant.now();

    calculateAndSaveForPeriod(PowerUserPeriod.DAILY, now.minus(1, ChronoUnit.DAYS), now);
    calculateAndSaveForPeriod(PowerUserPeriod.WEEKLY, now.minus(7, ChronoUnit.DAYS), now);
    calculateAndSaveForPeriod(PowerUserPeriod.MONTHLY, now.minus(30, ChronoUnit.DAYS), now);
    calculateAndSaveForPeriod(PowerUserPeriod.ALL_TIME, Instant.EPOCH, now);
  }

  private void calculateAndSaveForPeriod(PowerUserPeriod period, Instant startDate, Instant endDate) {
    powerUserRankingRepository.deleteByPeriod(period);

    Map<UUID, BigDecimal> reviewScores = powerUserRankingRepository.getReviewPopularScoreSum(startDate, endDate);
    Map<UUID, Long> likeCounts = powerUserRankingRepository.getLikeCount(startDate, endDate);
    Map<UUID, Long> commentCounts = powerUserRankingRepository.getCommentCount(startDate, endDate);

    List<UserScore> userScores = calculateScores(reviewScores, likeCounts, commentCounts);

    if (userScores.isEmpty()) return;

    List<PowerUserRanking> rankings = assignRankings(userScores, period);
    powerUserRankingRepository.saveAll(rankings);
  }

  public List<UserScore> calculateScores(Map<UUID, BigDecimal> reviewScores, Map<UUID, Long> likeCounts, Map<UUID, Long> commentCounts) {
    Set<UUID> activeUserIds = new HashSet<>();
    activeUserIds.addAll(reviewScores.keySet());
    activeUserIds.addAll(likeCounts.keySet());
    activeUserIds.addAll(commentCounts.keySet());

    List<UserScore> userScores = new ArrayList<>();
    BigDecimal reviewWeight = new BigDecimal("0.5");
    BigDecimal likeWeight = new BigDecimal("0.2");
    BigDecimal commentWeight = new BigDecimal("0.3");

    for (UUID userId : activeUserIds) {
      BigDecimal rScore = reviewScores.getOrDefault(userId, BigDecimal.ZERO).multiply(reviewWeight);
      BigDecimal lScore = BigDecimal.valueOf(likeCounts.getOrDefault(userId, 0L)).multiply(likeWeight);
      BigDecimal cScore = BigDecimal.valueOf(commentCounts.getOrDefault(userId, 0L)).multiply(commentWeight);

      BigDecimal totalScore = rScore.add(lScore).add(cScore);

      if (totalScore.compareTo(BigDecimal.ZERO) > 0) {
        userScores.add(new UserScore(userId, totalScore));
      }
    }
    return userScores;
  }

  public List<PowerUserRanking> assignRankings(List<UserScore> userScores, PowerUserPeriod period) {
    List<UserScore> sortedScores = new ArrayList<>(userScores);
    sortedScores.sort((a, b) -> b.score().compareTo(a.score()));

    List<PowerUserRanking> rankings = new ArrayList<>();
    for (int i = 0; i < sortedScores.size(); i++) {
      UserScore userScore = sortedScores.get(i);
      rankings.add(PowerUserRanking.builder()
          .userId(userScore.userId())
          .period(period)
          .score(userScore.score())
          .ranking(i + 1)
          .build());
    }
    return rankings;
  }
}
