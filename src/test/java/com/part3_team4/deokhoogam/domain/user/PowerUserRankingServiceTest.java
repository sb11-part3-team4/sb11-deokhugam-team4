package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PowerUserRankingServiceTest {

  @Mock
  private PowerUserRankingRepository powerUserRankingRepository;

  @InjectMocks
  private PowerUserRankingService powerUserRankingService;

  @Captor
  private ArgumentCaptor<List<PowerUserRanking>> rankingListCaptor;

  @Test
  @DisplayName("메인 로직: 기존 데이터를 지우고, 계산과 정렬을 거쳐 랭킹을 DB에 일괄 저장한다")
  void calculateAndSaveDailyRanking() {

    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    // 가짜 DB 조회 결과 세팅 (user1은 15점, user2는 1점 예상)
    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20));
    Map<UUID, Long> likeCounts = Map.of(user1, 10L, user2, 5L);
    Map<UUID, Long> commentCounts = Map.of(user1, 10L);

    when(powerUserRankingRepository.getReviewPopularScoreSum(any(), any())).thenReturn(
        reviewScores);
    when(powerUserRankingRepository.getLikeCount(any(), any())).thenReturn(likeCounts);
    when(powerUserRankingRepository.getCommentCount(any(), any())).thenReturn(commentCounts);

    powerUserRankingService.calculateAndSaveDailyRanking();

    // 기존 데이터 삭제 메서드가 불렸는지 확인
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.DAILY);

    //  DB에 저장하려던 List에서 올바르게 2명이 저장되었는지, 1등과 2등이 맞는지 확인
    verify(powerUserRankingRepository).saveAll(rankingListCaptor.capture());
    List<PowerUserRanking> savedRankings = rankingListCaptor.getValue();

    assertThat(savedRankings).hasSize(2);
    assertThat(savedRankings.get(0).getUserId()).isEqualTo(user1); // 15점 유저가 1등
    assertThat(savedRankings.get(1).getUserId()).isEqualTo(user2); // 1점 유저가 2등
  }

  @Test
  @DisplayName("계산 로직")
  void calculateScores() {

    UUID user1 = UUID.randomUUID(); // 활동 있음 (15.0점 예상)
    UUID user2 = UUID.randomUUID(); // 활동 적음 (1.0점 예상)
    UUID user3 = UUID.randomUUID(); // 0점 (결과에서 필터링되어야 함)

    // (리뷰 * 0.5) + (좋아요 * 0.2) + (댓글 * 0.3)
    // user1: 리뷰 20(10점) + 좋아요 10(2점) + 댓글 10(3점) = 총 15.0점
    // user2: 리뷰 0(0점) + 좋아요 5(1점) + 댓글 0(0점) = 총 1.0점
    // user3: 댓글 0(0점) = 총 0.0점
    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20));
    Map<UUID, Long> likeCounts = Map.of(user1, 10L, user2, 5L);
    Map<UUID, Long> commentCounts = Map.of(user1, 10L, user3, 0L);

    List<PowerUserRankingService.UserScore> result = powerUserRankingService.calculateScores(
        reviewScores, likeCounts, commentCounts);

    // 0점인 user3는 걸러지고 2명만 남음
    assertThat(result).hasSize(2);

    // 리스트에 user1과 user2가 모두 정상 계산되어 들어갔는지 확인
    assertThat(result).extracting("userId").containsExactlyInAnyOrder(user1, user2);
  }

  @Test
  @DisplayName("랭킹 로직 - 점수를 내림차순으로 정렬하고 1등부터 순위 부여")
  void assignRankings() {
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    List<PowerUserRankingService.UserScore> unsortedScores = List.of(
        new PowerUserRankingService.UserScore(user1, 10.0),
        new PowerUserRankingService.UserScore(user2, 20.0)
    );

    List<PowerUserRanking> result = powerUserRankingService.assignRankings(unsortedScores,
        PowerUserPeriod.DAILY);

    assertThat(result).hasSize(2);

    // 1등 검증 (user2, 20.0점)
    assertThat(result.get(0).getUserId()).isEqualTo(user2);
    assertThat(result.get(0).getScore()).isEqualTo(20.0);
    assertThat(result.get(0).getRanking()).isEqualTo(1);

    // 2등 검증 (user1, 10.0점)
    assertThat(result.get(1).getUserId()).isEqualTo(user1);
    assertThat(result.get(1).getScore()).isEqualTo(10.0);
    assertThat(result.get(1).getRanking()).isEqualTo(2);
  }
}
