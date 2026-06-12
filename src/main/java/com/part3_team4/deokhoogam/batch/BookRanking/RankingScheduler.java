package com.part3_team4.deokhoogam.batch.BookRanking;

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
public class RankingScheduler {

  private final JobLauncher jobLauncher;
  private final Job rankingJob;

  // 매시간 정각에 실행 (초 분 시 일 월 요일) -> 임시로 20초마다로 설정. 이후 메인 발표 종료 후 매시간 정각으로 수정
  @Scheduled(cron = "0/20 * * * * *")
  public void runRankingJob() throws Exception {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())  // 매번 다른 파라미터
          .toJobParameters();

      jobLauncher.run(rankingJob, params);

    } catch (Exception e) {
      log.error("랭킹 배치 실행 요청 실패", e);
      throw e;
    }
  }
}
