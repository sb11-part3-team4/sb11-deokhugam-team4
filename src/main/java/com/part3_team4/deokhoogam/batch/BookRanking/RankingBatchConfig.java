package com.part3_team4.deokhoogam.batch.BookRanking;


import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
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
public class RankingBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RankingCalculator rankingCalculator;

  // Job: 전체 작업 봉투
  @Bean
  public Job rankingJob() {
    return new JobBuilder("rankingJob", jobRepository)
        .start(rankingStep())
        .build();
  }

  // Step: Tasklet 하나로 구성
  @Bean
  public Step rankingStep() {
    return new StepBuilder("rankingStep", jobRepository)
        .tasklet(rankingTasklet(), transactionManager)
        .build();
  }

  // Tasklet: 4개 기간에 대해 calculateAndSave 실행
  @Bean
  public Tasklet rankingTasklet() {
    return (contribution, chunkContext) -> {
      log.info("인기 도서 랭킹 산출 시작");
      for (PeriodType period : PeriodType.values()) {
        rankingCalculator.calculateAndSave(period);
      }
      log.info("인기 도서 랭킹 산출 종료");
      return RepeatStatus.FINISHED;
    };
  }
}
