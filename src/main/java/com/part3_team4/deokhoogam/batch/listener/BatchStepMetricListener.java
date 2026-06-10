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

    // Spring Batch가 자동으로 세어둔 "삭제(=쓴) 건수"
    long deleted = stepExecution.getWriteCount();

    customMetrics.recordCount(jobName, null, deleted);  // 0건이어도 자동 기록됨
    return stepExecution.getExitStatus();
  }
}