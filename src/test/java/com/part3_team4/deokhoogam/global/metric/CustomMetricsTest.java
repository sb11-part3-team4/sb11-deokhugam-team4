package com.part3_team4.deokhoogam.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.global.metric.CustomMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomMetricsTest {

  private SimpleMeterRegistry registry;
  private CustomMetrics customMetrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    customMetrics = new CustomMetrics(registry);
  }

  @Nested
  @DisplayName("recordCount - 처리 건수 기록")
  class RecordCount {

    @Test
    @DisplayName("기록한 건수가 게이지 값으로 조회된다")
    void 건수가_기록된다() {
      customMetrics.recordCount("rankingJob", "DAILY", 50);

      double value = registry.get("batch.processed.count")
          .tag("job", "rankingJob")
          .tag("period", "DAILY")
          .gauge()
          .value();

      assertThat(value).isEqualTo(50);
    }

    @Test
    @DisplayName("같은 job·period를 두 번 기록하면 누적이 아니라 최신값으로 덮어쓴다")
    void 누적이_아니라_덮어쓴다() {
      customMetrics.recordCount("rankingJob", "DAILY", 50);
      customMetrics.recordCount("rankingJob", "DAILY", 30);

      double value = registry.get("batch.processed.count")
          .tag("job", "rankingJob")
          .tag("period", "DAILY")
          .gauge()
          .value();

      // Counter였다면 80, Gauge라 최신값 30
      assertThat(value).isEqualTo(30);
    }

    @Test
    @DisplayName("period가 있으면 period 태그가 붙는다")
    void period_tag_attached_when_period_is_not_null() {
      customMetrics.recordCount("rankingJob", "MONTHLY", 10);

      Gauge gauge = registry.get("batch.processed.count")
          .tag("job", "rankingJob")
          .tag("period", "MONTHLY")
          .gauge();

      assertThat(gauge.getId().getTag("period")).isEqualTo("MONTHLY");
    }

    @Test
    @DisplayName("period가 null이면 period 태그 없이 job 태그만 붙는다")
    void period_tag_is_not_attached_when_period_is_null() {
      customMetrics.recordCount("deleteOrphanNotificationJob", null, 7);

      Gauge gauge = registry.get("batch.processed.count")
          .tag("job", "deleteOrphanNotificationJob")
          .gauge();

      assertThat(gauge.value()).isEqualTo(7);
      // period 태그는 등록되지 않아야 한다
      assertThat(gauge.getId().getTag("period")).isNull();
    }

    @Test
    @DisplayName("job 또는 period가 다르면 서로 다른 게이지로 분리된다")
    void different_gauge_is_separated_when_job_or_period_is_different() {
      customMetrics.recordCount("rankingJob", "DAILY", 50);
      customMetrics.recordCount("rankingJob", "WEEKLY", 20);

      double daily = registry.get("batch.processed.count")
          .tag("job", "rankingJob").tag("period", "DAILY").gauge().value();
      double weekly = registry.get("batch.processed.count")
          .tag("job", "rankingJob").tag("period", "WEEKLY").gauge().value();

      assertThat(daily).isEqualTo(50);
      assertThat(weekly).isEqualTo(20);
    }

    @Test
    @DisplayName("0건도 기록된다 (배치가 돌았으나 대상이 없는 경우)")
    void registered_even_if_count_is_zero() {
      customMetrics.recordCount("deleteOrphanNotificationJob", null, 0);

      double value = registry.get("batch.processed.count")
          .tag("job", "deleteOrphanNotificationJob")
          .gauge()
          .value();

      assertThat(value).isZero();
    }
  }

  @Nested
  @DisplayName("recordLastSuccess - 마지막 성공 시각 기록")
  class RecordLastSuccess {

    @Test
    @DisplayName("성공 시각이 0보다 큰 타임스탬프로 기록된다")
    void 성공시각이_기록된다() {
      long before = System.currentTimeMillis();
      customMetrics.recordLastSuccess("rankingJob", "DAILY");
      long after = System.currentTimeMillis();

      double value = registry.get("batch.last.success.time")
          .tag("job", "rankingJob")
          .tag("period", "DAILY")
          .gauge()
          .value();

      // 호출 전후 시각 사이의 값이어야 한다
      assertThat(value).isBetween((double) before, (double) after);
    }

    @Test
    @DisplayName("다시 호출하면 더 최신 시각으로 갱신된다")
    void 다시_호출하면_갱신된다() throws InterruptedException {
      customMetrics.recordLastSuccess("rankingJob", "DAILY");
      double first = registry.get("batch.last.success.time")
          .tag("job", "rankingJob").tag("period", "DAILY").gauge().value();

      Thread.sleep(5); // 시각이 확실히 달라지도록 잠깐 대기
      customMetrics.recordLastSuccess("rankingJob", "DAILY");
      double second = registry.get("batch.last.success.time")
          .tag("job", "rankingJob").tag("period", "DAILY").gauge().value();

      assertThat(second).isGreaterThanOrEqualTo(first);
    }

    @Test
    @DisplayName("period가 null이면 period 태그 없이 기록된다 (유저 삭제 등)")
    void period가_null이면_태그가_없다() {
      customMetrics.recordLastSuccess("deleteExpiredUserJob", null);

      Gauge gauge = registry.get("batch.last.success.time")
          .tag("job", "deleteExpiredUserJob")
          .gauge();

      assertThat(gauge.value()).isGreaterThan(0);
      assertThat(gauge.getId().getTag("period")).isNull();
    }
  }
}