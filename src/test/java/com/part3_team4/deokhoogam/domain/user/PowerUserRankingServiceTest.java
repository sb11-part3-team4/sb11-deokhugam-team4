package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PowerUserRankingServiceTest {

  @Mock
  private PowerUserRankingRepository powerUserRankingRepository;

  @Mock
  private UserService userService;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ObjectMapper redisObjectMapper;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private PowerUserRankingService powerUserRankingService;

  @Captor
  private ArgumentCaptor<List<PowerUserRanking>> rankingListCaptor;

  @BeforeEach
  void setUp() {
    // 테스트 기본 설정: 캐시를 활성화(true) 상태로 둡니다.
    ReflectionTestUtils.setField(powerUserRankingService, "cacheEnabled", true);
  }

  @Test
  @DisplayName("메인 로직: 4가지 기간에 대해 각각 기존 데이터를 지우고, 계산된 랭킹을 DB에 일괄 저장 및 Redis 캐시 삭제")
  void calculateAndSaveAllRanking() {
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20));
    Map<UUID, Long> likeCounts = Map.of(user1, 10L, user2, 5L);
    Map<UUID, Long> commentCounts = Map.of(user1, 10L);

    when(powerUserRankingRepository.getReviewPopularScoreSum(any(), any())).thenReturn(reviewScores);
    when(powerUserRankingRepository.getLikeCount(any(), any())).thenReturn(likeCounts);
    when(powerUserRankingRepository.getCommentCount(any(), any())).thenReturn(commentCounts);

    powerUserRankingService.calculateAndSaveAllRankings();

    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.DAILY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.WEEKLY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.MONTHLY);
    verify(powerUserRankingRepository).deleteByPeriod(PowerUserPeriod.ALL_TIME);

    verify(powerUserRankingRepository, times(4)).saveAll(rankingListCaptor.capture());
    // 각 기간별 캐시 키 삭제 여부 검증
    verify(redisTemplate).delete("ranking:user:DAILY");
    verify(redisTemplate).delete("ranking:user:WEEKLY");
    verify(redisTemplate).delete("ranking:user:MONTHLY");
    verify(redisTemplate).delete("ranking:user:ALL_TIME");

    List<List<PowerUserRanking>> allCapturedRankings = rankingListCaptor.getAllValues();
    assertThat(allCapturedRankings).hasSize(4);
    assertThat(allCapturedRankings.get(0).get(0).getPeriod()).isEqualTo(PowerUserPeriod.DAILY);
  }

  @Test
  @DisplayName("점수 계산 로직")
  void calculateScores() {
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();
    UUID user3 = UUID.randomUUID();

    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20));
    Map<UUID, Long> likeCounts = Map.of(user1, 10L, user2, 5L);
    Map<UUID, Long> commentCounts = Map.of(user1, 10L, user3, 0L);

    List<PowerUserRankingService.UserScore> result = powerUserRankingService.calculateScores(
        reviewScores, likeCounts, commentCounts);

    assertThat(result).hasSize(2);
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

    List<PowerUserRanking> result = powerUserRankingService.assignRankings(unsortedScores, PowerUserPeriod.DAILY);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getUserId()).isEqualTo(user2);
    assertThat(result.get(1).getUserId()).isEqualTo(user1);
  }

  @Test
  @DisplayName("랭킹 집계 예외 - 활용 데이터가 하나도 없으면 랭킹을 부여하지 않고 종료")
  void calculateAndSaveDailyRanking_empty() {
    when(powerUserRankingRepository.getReviewPopularScoreSum(any(), any())).thenReturn(Map.of());
    when(powerUserRankingRepository.getLikeCount(any(), any())).thenReturn(Map.of());
    when(powerUserRankingRepository.getCommentCount(any(), any())).thenReturn(Map.of());

    powerUserRankingService.calculateAndSaveAllRankings();

    verify(powerUserRankingRepository, times(4)).deleteByPeriod(any(PowerUserPeriod.class));
    verify(powerUserRankingRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("캐시 히트 - Redis에 데이터가 존재하면 DB 조회를 생략하고 반환")
  void getRankingWithNickname_CacheHit() throws Exception {
    String cachedData = "[{\"userId\":\"...\",\"nickname\":\"테스트\",\"ranking\":1,\"score\":10.0}]";
    List<PowerUserRankingResponseDto> expectedResponse = List.of(
        new PowerUserRankingResponseDto(UUID.randomUUID(), "테스트", 1, 10.0)
    );

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("ranking:user:DAILY")).thenReturn(cachedData);
    when(redisObjectMapper.readValue(eq(cachedData), any(TypeReference.class))).thenReturn(expectedResponse);

    List<PowerUserRankingResponseDto> result = powerUserRankingService.getRankingWithNickname(PowerUserPeriod.DAILY, 10);

    assertThat(result).isEqualTo(expectedResponse);
    verify(powerUserRankingRepository, never()).findByPeriodOrderByRankingAsc(any(), any()); // DB 조회 발생 안 함
    verify(userService, never()).getUserNicknames(any());
  }

  @Test
  @DisplayName("캐시 미스 - Redis에 데이터가 없으면 DB 조회 후 Redis에 저장")
  void getRankingWithNickname_CacheMiss() throws Exception {
    UUID user1 = UUID.randomUUID();
    PowerUserRanking ranking1 = PowerUserRanking.builder().userId(user1).period(PowerUserPeriod.DAILY).score(BigDecimal.valueOf(20.8)).ranking(1).build();
    String jsonResult = "[{\"userId\":\"...\",\"nickname\":\"정상유저\",\"ranking\":1,\"score\":20.0}]";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("ranking:user:DAILY")).thenReturn(null); // 캐시 미스

    when(powerUserRankingRepository.findByPeriodOrderByRankingAsc(eq(PowerUserPeriod.DAILY), any(Pageable.class)))
        .thenReturn(List.of(ranking1));
    when(userService.getUserNicknames(List.of(user1))).thenReturn(Map.of(user1, "정상유저"));
    when(redisObjectMapper.writeValueAsString(any())).thenReturn(jsonResult);

    List<PowerUserRankingResponseDto> result = powerUserRankingService.getRankingWithNickname(PowerUserPeriod.DAILY, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).nickname()).isEqualTo("정상유저");
    assertThat(result.get(0).score()).isEqualTo(20.0);

    // DB 조회 후 캐시에 저장되는지 검증
    verify(valueOperations).set(eq("ranking:user:DAILY"), eq(jsonResult), any(Duration.class));
  }

  @Test
  @DisplayName("Redis 연결 오류 발생 시 - 캐시 조회에 실패해도 DB에서 정상적으로 데이터를 가져온다")
  void getRankingWithNickname_RedisException_DBFallback() {
    UUID user1 = UUID.randomUUID();
    PowerUserRanking ranking1 = PowerUserRanking.builder().userId(user1).period(PowerUserPeriod.DAILY).score(BigDecimal.valueOf(15.5)).ranking(1).build();

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis Error")); // 강제 예외 발생

    when(powerUserRankingRepository.findByPeriodOrderByRankingAsc(eq(PowerUserPeriod.DAILY), any(Pageable.class)))
        .thenReturn(List.of(ranking1));
    when(userService.getUserNicknames(List.of(user1))).thenReturn(Map.of(user1, "정상유저"));

    List<PowerUserRankingResponseDto> result = powerUserRankingService.getRankingWithNickname(PowerUserPeriod.DAILY, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).nickname()).isEqualTo("정상유저");
  }

  @Test
  @DisplayName("캐시 비활성화 상태 - 캐시 로직을 타지 않고 DB에서만 데이터를 가져온다")
  void getRankingWithNickname_CacheDisabled() {
    ReflectionTestUtils.setField(powerUserRankingService, "cacheEnabled", false); // 캐시 끄기

    UUID user1 = UUID.randomUUID();
    PowerUserRanking ranking1 = PowerUserRanking.builder().userId(user1).period(PowerUserPeriod.DAILY).score(BigDecimal.valueOf(15.5)).ranking(1).build();

    when(powerUserRankingRepository.findByPeriodOrderByRankingAsc(eq(PowerUserPeriod.DAILY), any(Pageable.class)))
        .thenReturn(List.of(ranking1));
    when(userService.getUserNicknames(List.of(user1))).thenReturn(Map.of(user1, "정상유저"));

    List<PowerUserRankingResponseDto> result = powerUserRankingService.getRankingWithNickname(PowerUserPeriod.DAILY, 10);

    assertThat(result).hasSize(1);
    verify(redisTemplate, never()).opsForValue(); // 레디스 호출 아예 안 함
  }

  @Test
  @DisplayName("DB 조회 결과가 빈 리스트면 빈 리스트를 반환하고 캐시에 저장하지 않는다")
  void getRankingWithNickname_EmptyRankings() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("ranking:user:DAILY")).thenReturn(null);

    when(powerUserRankingRepository.findByPeriodOrderByRankingAsc(eq(PowerUserPeriod.DAILY), any(Pageable.class)))
        .thenReturn(List.of());

    List<PowerUserRankingResponseDto> result = powerUserRankingService.getRankingWithNickname(PowerUserPeriod.DAILY, 10);

    assertThat(result).isEmpty();
    verify(userService, never()).getUserNicknames(any()); // 빈 리스트면 닉네임 조회 생략
    verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class)); // 빈 리스트는 캐시 저장 안함
  }
}