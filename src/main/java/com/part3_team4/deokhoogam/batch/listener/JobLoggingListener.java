package com.part3_team4.deokhoogam.batch.listener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobLoggingListener implements JobExecutionListener {

  private final Clock clock;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    Instant targetTime = Instant.now(clock).minus(1, ChronoUnit.DAYS);
    log.info("배치 시작 - Job: {}, baseDate/TargetTime: {}", jobName, targetTime);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    BatchStatus status = jobExecution.getStatus();

    long writeCount = jobExecution.getStepExecutions().stream()
        .mapToLong(StepExecution::getWriteCount)
        .sum();

    long durationMs = (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null)
        ? Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
        : -1L;

    if (status == BatchStatus.COMPLETED) {
      log.info("배치 완료 - Job: {}, 처리 건수: {}건, 소요시간: {}ms", jobName, writeCount, durationMs);

    } else if (status == BatchStatus.FAILED) {
      log.error("배치 실패 - Job: {}, 메인 예외 발생", jobName);
      jobExecution.getAllFailureExceptions()
          .forEach(e -> log.error("배치 실패 예외 내용: ", e));

    } else {
      log.warn("배치 비정상 종료 - Job: {}, 상태: {}", jobName, status);
    }
  }
}