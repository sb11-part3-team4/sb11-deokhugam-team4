package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PowerUserRankingService {

  private final UserService userService;
  private final PowerUserRankingRepository powerUserRankingRepository;

  public void calculateAndSaveAllRankings() {
  }

  public record UserScore(UUID userId, BigDecimal score) {

  }

  public List<PowerUserRankingResponseDto> getDailyRankingWithNickname() {
    List<PowerUserRanking> rankings = powerUserRankingRepository.findByPeriodOrderByRankingAsc(PowerUserPeriod.DAILY);

    return rankings.stream().map(ranking -> {
      UserResponse user = userService.getUser(ranking.getUserId());

      String nickname = (user != null) ? user.nickname() : "알수없음";

      return new PowerUserRankingResponseDto(
          ranking.getUserId(),
          nickname,
          ranking.getRanking(),
          ranking.getScore().doubleValue()
      );
    }).collect(Collectors.toList());
  }

  public List<UserScore> calculateScores(Map<UUID, BigDecimal> reviewScores,
      Map<UUID, Long> likeCounts,
      Map<UUID, Long> commentCounts) {
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
