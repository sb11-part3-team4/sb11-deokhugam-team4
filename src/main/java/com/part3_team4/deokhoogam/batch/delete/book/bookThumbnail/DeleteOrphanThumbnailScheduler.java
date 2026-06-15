package com.part3_team4.deokhoogam.batch.delete.book.bookThumbnail;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteOrphanThumbnailScheduler {

  private final JobLauncher jobLauncher;
  private final Job deleteOrphanThumbnailJob;

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  public void run() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLong("run.id", System.currentTimeMillis())
        .toJobParameters();
    jobLauncher.run(deleteOrphanThumbnailJob, params);
  }
}