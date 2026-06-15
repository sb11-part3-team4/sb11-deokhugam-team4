package com.part3_team4.deokhoogam.batch.listener;

import com.part3_team4.deokhoogam.global.metric.CustomMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobMetricListener implements JobExecutionListener {

  private final MeterRegistry meterRegistry;
  private final CustomMetrics customMetrics;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    jobExecution.getExecutionContext().putLong("startTime", System.currentTimeMillis());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();

    long duration = System.currentTimeMillis()
        - jobExecution.getExecutionContext().getLong("startTime");
    Timer.builder("batch.execution.duration").tag("job", jobName)
        .register(meterRegistry).record(duration, TimeUnit.MILLISECONDS);

    String status = jobExecution.getStatus() == BatchStatus.COMPLETED ? "success" : "failure";
    Counter.builder("batch.execution.count").tag("job", jobName).tag("status", status)
        .register(meterRegistry).increment();

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      customMetrics.recordLastSuccess(jobName, null);
    }
  }
}