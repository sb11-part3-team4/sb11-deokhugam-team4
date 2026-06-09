package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PowerUserRankingServiceTest {

  private PowerUserRankingService powerUserRankingService;

  @BeforeEach
  void setUp() {
    // 계산/정렬 로직만 테스트하기 위해 null 주입
    powerUserRankingService = new PowerUserRankingService(null);
  }

  @Test
  @DisplayName("계산 로직")
  void calculateScores() {

    UUID user1 = UUID.randomUUID(); // 활동 있음 (15.0점 예상)
    UUID user2 = UUID.randomUUID(); // 활동 없음 (0점 예상 -> 필터링되어야 함)

    // (리뷰 * 0.5) + (좋아요 * 0.2) + (댓글 * 0.3)
    // user1: 리뷰 20(10점) + 좋아요 10(2점) + 댓글 10(3점) = 총 15.0점
    // user2: 리뷰 0(0점) + 좋아요 5(1점) + 댓글 0(0점) = 총 1.0점
    Map<UUID, BigDecimal> reviewScores = Map.of(user1, BigDecimal.valueOf(20)); // 20 * 0.5 = 10점
    Map<UUID, Long> likeCounts = Map.of(user1, 10L); // 10 * 0.2 = 2점
    Map<UUID, Long> commentCounts = Map.of(user1, 10L); // 10 * 0.3 = 3점

    List<PowerUserRankingService.UserScore> result = powerUserRankingService.calculateScores(reviewScores, likeCounts, commentCounts);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo(user1);
    assertThat(result.get(0).score()).isEqualTo(15.0);
  }
}
