package com.part3_team4.deokhoogam.batch.userRanking;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerUserRankingScheduler {
  private final JobLauncher jobLauncher;
  private final Job powerUserRankingJob;

  @Scheduled(cron = "0/20 * * * * *")
  public void runPowerUserRankingJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(powerUserRankingJob, params);
  }
}
