package com.part3_team4.deokhoogam.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job deleteOrphanCommentJob;
    private final Job deleteOrphanReviewJob;

    @Scheduled(cron = "0 0 3 * * *")
    public void runDeleteOrphanCommentJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(deleteOrphanCommentJob, params);
        } catch (Exception e) {
            log.error("고아 댓글 삭제 배치 실행 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void runDeleteOrphanReviewJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(deleteOrphanReviewJob, params);
        } catch (Exception e) {
            log.error("고아 리뷰 삭제 배치 실행 중 오류 발생", e);
        }
    }
}