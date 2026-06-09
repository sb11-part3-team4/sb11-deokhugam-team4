package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import(PowerUserRankingRepositoryTest.TestConfig.class)
public class PowerUserRankingRepositoryTest {

  @TestConfiguration
  static class TestConfig {
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
      return new JPAQueryFactory(entityManager);
    }
  }

  @Autowired
  private PowerUserRankingRepository powerUserRankingRepository;

  @Test
  @DisplayName("Native Query 3종이 문법 오류 없이 정상적으로 실행되는지 검증")
  void nativeQueriesExecuteSuccessfully() {
    Instant now = Instant.now();
    Instant startDate = now.minus(1, ChronoUnit.DAYS);

    Map<UUID, BigDecimal> reviewScores = powerUserRankingRepository.getReviewPopularScoreSum(startDate, now);
    Map<UUID, Long> likeCounts = powerUserRankingRepository.getLikeCount(startDate, now);
    Map<UUID, Long> commentCounts = powerUserRankingRepository.getCommentCount(startDate, now);

    // 에러 없이 빈 리스트라도 정상 반환하는지 확인
    assertThat(reviewScores).isNotNull();
    assertThat(likeCounts).isNotNull();
    assertThat(commentCounts).isNotNull();
  }

  @Test
  @DisplayName("특정 기간(Period)의 파워 유저 랭킹 데이터를 일괄 삭제할 수 있다")
  void deleteByPeriod() {
    PowerUserRanking ranking1 = PowerUserRanking.builder()
        .userId(UUID.randomUUID())
        .period(PowerUserPeriod.DAILY)
        .score(10.0)
        .ranking(1)
        .build();
    powerUserRankingRepository.save(ranking1);

    powerUserRankingRepository.deleteByPeriod(PowerUserPeriod.DAILY);

    List<PowerUserRanking> result = powerUserRankingRepository.findAll();
    assertThat(result).isEmpty();
  }
}
