package com.part3_team4.deokhoogam.batch.userRanking;

import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
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

  @Bean
  public Job powerUserRankingJob() {
    return new JobBuilder("powerUserRankingJob", jobRepository)
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
      powerUserRankingService.calculateAndSaveAllRankings();

      return RepeatStatus.FINISHED;
    };
  }
}
