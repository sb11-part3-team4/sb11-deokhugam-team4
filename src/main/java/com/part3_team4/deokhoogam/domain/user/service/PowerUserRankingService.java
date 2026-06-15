package com.part3_team4.deokhoogam.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class PowerUserRankingService {

  private final UserService userService;
  private final PowerUserRankingRepository powerUserRankingRepository;

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper redisObjectMapper;

  @Value("${cache.ranking.enabled:true}")
  private boolean cacheEnabled;

  public record UserScore(UUID userId, BigDecimal score) {}

  public List<PowerUserRankingResponseDto> getRankingWithNickname(PowerUserPeriod period, int limit) {
    String key = "ranking:user:" + period;

    // 캐시 조회
    if (cacheEnabled) {
      try {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
          log.debug("파워유저 랭킹 캐시 히트: key={}", key);
          return redisObjectMapper.readValue(
              cached, new TypeReference<>() {}
          );
        }
        log.debug("파워유저 랭킹 캐시 미스: key={}", key);
      } catch (Exception e) {
        log.warn("파워유저 랭킹 캐시 읽기 실패, DB 폴백: key={}", key, e);
      }
    }

    // PageRequest를 생성하여 최대 limit 개수만큼만 가져오도록 설정
    Pageable pageable = PageRequest.of(0, limit);
    List<PowerUserRanking> rankings = powerUserRankingRepository.findByPeriodOrderByRankingAsc(period, pageable);

    if (rankings.isEmpty()) {
      return List.of();
    }

    // 랭킹에 등록된 유저 ID 일괄 추출
    List<UUID> userIds = rankings.stream()
        .map(PowerUserRanking::getUserId)
        .collect(Collectors.toList());

    // IN 절 쿼리 단 1번으로 닉네임 일괄 매핑 (N+1 최적화)
    Map<UUID, String> nicknameMap = userService.getUserNicknames(userIds);

    List<PowerUserRankingResponseDto> result = rankings.stream().map(ranking -> {
      String nickname = nicknameMap.getOrDefault(ranking.getUserId(), "알수없음");
      double flooredScore = Math.floor(ranking.getScore().doubleValue());

      return new PowerUserRankingResponseDto(
          ranking.getUserId(), nickname, ranking.getRanking(), flooredScore
      );
    }).collect(Collectors.toList());

    // 캐시 저장
    if (cacheEnabled) {
      try {
        redisTemplate.opsForValue().set(
            key, redisObjectMapper.writeValueAsString(result), java.time.Duration.ofHours(2));
      } catch (Exception e) {
        log.warn("파워유저 랭킹 캐시 저장 실패: key={}", key, e);
      }
    }

    return result;
  }

  @Transactional
  public void calculateAndSaveAllRankings() {
    for (PowerUserPeriod period : PowerUserPeriod.values()) {
      calculateAndSaveForPeriod(period);   // ← 새 메서드 재사용
    }
  }

  private int calculateAndSaveForPeriod(PowerUserPeriod period, Instant startDate, Instant endDate) {
    powerUserRankingRepository.deleteByPeriod(period);

    Map<UUID, BigDecimal> reviewScores = powerUserRankingRepository.getReviewPopularScoreSum(startDate, endDate);
    Map<UUID, Long> likeCounts = powerUserRankingRepository.getLikeCount(startDate, endDate);
    Map<UUID, Long> commentCounts = powerUserRankingRepository.getCommentCount(startDate, endDate);

    List<UserScore> userScores = calculateScores(reviewScores, likeCounts, commentCounts);

    if (userScores.isEmpty()) return 0;

    List<PowerUserRanking> rankings = assignRankings(userScores, period);
    powerUserRankingRepository.saveAll(rankings);
    return rankings.size();
  }

  @Transactional
  public int calculateAndSaveForPeriod(PowerUserPeriod period) {
    Instant now = Instant.now();
    Instant start = switch (period) {
      case DAILY    -> now.minus(1, ChronoUnit.DAYS);
      case WEEKLY   -> now.minus(7, ChronoUnit.DAYS);
      case MONTHLY  -> now.minus(30, ChronoUnit.DAYS);
      case ALL_TIME -> Instant.EPOCH;
    };
    int saved = calculateAndSaveForPeriod(period, start, now);
    redisTemplate.delete("ranking:user:" + period);
    return saved;
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