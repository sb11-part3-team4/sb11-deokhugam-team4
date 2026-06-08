package com.part3_team4.deokhoogam.batch.BookRanking;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingScheduler {

  private final JobLauncher jobLauncher;
  private final Job rankingJob;

  // 매시간 정각에 실행 (초 분 시 일 월 요일)
  @Scheduled(cron = "0/20 * * * * *")
  public void runRankingJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLong("timestamp", System.currentTimeMillis())  // 매번 다른 파라미터
        .toJobParameters();

    jobLauncher.run(rankingJob, params);
  }
}
