package com.part3_team4.deokhoogam.batch.userRanking;

import com.part3_team4.deokhoogam.batch.listener.BatchJobMetricListener;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import com.part3_team4.deokhoogam.global.metric.CustomMetrics;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PowerUserRankingBatchConfig {
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PowerUserRankingService powerUserRankingService;
  private final BatchJobMetricListener batchJobMetricListener;
  private final CustomMetrics customMetrics;



  @Bean
  public Job powerUserRankingJob() {
    return new JobBuilder("powerUserRankingJob", jobRepository)
        .listener(batchJobMetricListener)
        .start(powerUserRankingStep())
        .build();
  }

  @Bean
  public Step powerUserRankingStep() {
    return new StepBuilder("powerUserRankingStep", jobRepository)
        .tasklet(powerUserRankingTasklet(), transactionManager)
        .build();
  }

  @Bean
  public Tasklet powerUserRankingTasklet() {
    return (contribution, chunkContext) -> {
      log.info("파워 유저 랭킹 산정 시작. 실행 시각: {}", Instant.now());
      try {
        for (PowerUserPeriod period : PowerUserPeriod.values()) {
          int saved = powerUserRankingService.calculateAndSaveForPeriod(period);
          customMetrics.recordCount("powerUserRankingJob", period.name(), saved);
        }
        log.info("파워 유저 랭킹 산정 완료");
        return RepeatStatus.FINISHED;
      } catch (Exception e) {
        log.error("파워 유저 랭킹 산정 실패", e);
        throw e;
      }
    };
  }
}
