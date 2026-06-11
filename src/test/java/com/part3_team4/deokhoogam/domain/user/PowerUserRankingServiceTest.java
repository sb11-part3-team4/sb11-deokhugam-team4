package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
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

  @Mock
  private UserService userService;

  @Test
  @DisplayName("메인 로직: 4가지 기간에 대해 각각 기존 데이터를 지우고, 계산된 랭킹을 DB에 일괄 저장")
  void calculateAndSaveAllRanking() {

    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    // 가짜 DB 조회 결과 세팅 (user1은 15점, user2는 1점 예상)
    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20));
    Map<UUID, Long> likeCounts = Map.of(user1, 10L, user2, 5L);
    Map<UUID, Long> commentCounts = Map.of(user1, 10L);

    when(powerUserRankingRepository.getReviewPopularScoreSum(any(), any())).thenReturn(reviewScores);
    when(powerUserRankingRepository.getLikeCount(any(), any())).thenReturn(likeCounts);
    when(powerUserRankingRepository.getCommentCount(any(), any())).thenReturn(commentCounts);

    powerUserRankingService.calculateAndSaveAllRankings();

    // 4가지 기간에 대해 각각 기존 데이터를 지웠는지 명시적으로 검증
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.DAILY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.WEEKLY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.MONTHLY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.ALL_TIME);

    verify(powerUserRankingRepository, times(4))
        .saveAll(rankingListCaptor.capture());

    List<List<PowerUserRanking>> allCapturedRankings = rankingListCaptor.getAllValues();
    assertThat(allCapturedRankings).hasSize(4);

    // 각 호출마다 저장된 랭킹 데이터의 기간(period) 값이 올바른지 검증
    assertThat(allCapturedRankings.get(0).get(0).getPeriod()).isEqualTo(PowerUserPeriod.DAILY);
    assertThat(allCapturedRankings.get(1).get(0).getPeriod()).isEqualTo(PowerUserPeriod.WEEKLY);
    assertThat(allCapturedRankings.get(2).get(0).getPeriod()).isEqualTo(PowerUserPeriod.MONTHLY);
    assertThat(allCapturedRankings.get(3).get(0).getPeriod()).isEqualTo(PowerUserPeriod.ALL_TIME);

    // 가장 마지막에 캡처된(ALL_TIME) 저장 리스트로 점수/등수 검증
    List<PowerUserRanking> savedRankings = allCapturedRankings.get(3);

    assertThat(savedRankings).hasSize(2);
    assertThat(savedRankings.get(0).getUserId()).isEqualTo(user1); // 15점 유저가 1등
    assertThat(savedRankings.get(1).getUserId()).isEqualTo(user2); // 1점 유저가 2등
  }

  @Test
  @DisplayName("점수 계산 로직")
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
  @DisplayName("랭킹 부여 로직 - 점수를 내림차순으로 정렬하고 1등부터 순위 부여")
  void assignRankings() {
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    List<PowerUserRankingService.UserScore> unsortedScores = List.of(
        new PowerUserRankingService.UserScore(user1, BigDecimal.valueOf(10.0)),
        new PowerUserRankingService.UserScore(user2, BigDecimal.valueOf(20.0))
    );

    List<PowerUserRanking> result = powerUserRankingService.assignRankings(unsortedScores,
        PowerUserPeriod.DAILY);

    assertThat(result).hasSize(2);

    // 1등 검증 (user2, 20.0점)
    assertThat(result.get(0).getUserId()).isEqualTo(user2);
    assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
    assertThat(result.get(0).getRanking()).isEqualTo(1);

    // 2등 검증 (user1, 10.0점)
    assertThat(result.get(1).getUserId()).isEqualTo(user1);
    assertThat(result.get(1).getScore()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    assertThat(result.get(1).getRanking()).isEqualTo(2);
  }

  @Test
  @DisplayName("랭킹 조회 - 유저 정보가 존재하면 닉네임을, 없으면 '알수없음' 반환 및 소수점 버림 로직 검증")
  void getRankingWithNickname() {
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    // 소수점이 있는 점수로 세팅하여 버림 처리가 잘 되는지 확인
    PowerUserRanking ranking1 = PowerUserRanking.builder().userId(user1)
        .period(PowerUserPeriod.DAILY).score(BigDecimal.valueOf(20.8)).ranking(1).build();
    PowerUserRanking ranking2 = PowerUserRanking.builder().userId(user2)
        .period(PowerUserPeriod.DAILY).score(BigDecimal.valueOf(10.4)).ranking(2).build();

    when(powerUserRankingRepository.findByPeriodOrderByRankingAsc(PowerUserPeriod.DAILY))
        .thenReturn(List.of(ranking1, ranking2));

    //user1은 정상 유저, user2는 탈퇴 등으로 null을 반환한다고 가정
    when(userService.getUser(user1)).thenReturn(new UserResponse("test@email.com", "정상유저"));
    when(userService.getUser(user2)).thenReturn(null);

    List<PowerUserRankingResponseDto> result = powerUserRankingService
        .getRankingWithNickname(PowerUserPeriod.DAILY);

    assertThat(result).hasSize(2);

    // 유저1: 닉네임 반환 및 20.8 -> 20.0 소수점 버림 확인
    assertThat(result.get(0).nickname()).isEqualTo("정상유저");
    assertThat(result.get(0).score()).isEqualTo(20.0);

    // 유저2: 닉네임 반환 및 10.4 -> 10.0 소수점 버림 확인
    assertThat(result.get(1).nickname()).isEqualTo("알수없음");
    assertThat(result.get(1).score()).isEqualTo(10.0);
  }

  @Test
  @DisplayName("랭킹 집계 예외 - 활용 데이터가 하나도 없으면 랭킹을 부여하지 않고 종료")
  void calculateAndSaveDailyRanking_empty() {
    //모든 쿼리가 빈 데이터를 반환한다고 가정
    when(powerUserRankingRepository.getReviewPopularScoreSum(any(), any())).thenReturn(Map.of());
    when(powerUserRankingRepository.getLikeCount(any(), any())).thenReturn(Map.of());
    when(powerUserRankingRepository.getCommentCount(any(), any())).thenReturn(Map.of());

    powerUserRankingService.calculateAndSaveAllRankings();

    // 기존 데이터 삭제는 4개 기간에 대해 무조건 실행
    verify(powerUserRankingRepository, times(4))
        .deleteByPeriod(any(PowerUserPeriod.class));
    // 데이터가 없으므로 DB 저장은 한 번도 실행되지 않아야 함
    verify(powerUserRankingRepository, never()).saveAll(any());
  }
}
