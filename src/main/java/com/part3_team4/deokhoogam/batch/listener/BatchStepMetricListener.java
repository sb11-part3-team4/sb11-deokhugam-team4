package com.part3_team4.deokhoogam.batch.listener;

import com.part3_team4.deokhoogam.global.metric.CustomMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchStepMetricListener implements StepExecutionListener {

  private final CustomMetrics customMetrics;

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

    long detected = stepExecution.getReadCount();    // reader가 읽은 = 찾은 고아 수
    long deleted  = stepExecution.getWriteCount();   // writer가 쓴 = 지운 수

    customMetrics.recordGauge("batch.orphan.count", jobName, "detected", detected);
    customMetrics.recordGauge("batch.orphan.count", jobName, "deleted", deleted);

    return stepExecution.getExitStatus();
  }
}