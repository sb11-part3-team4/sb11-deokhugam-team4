package com.part3_team4.deokhoogam.batch.delete.notification;

import com.part3_team4.deokhoogam.domain.notification.service.NotificationAutoDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * 만료된 알림을 주기적으로 삭제하는 배치 실행 클래스입니다.
 * <p>
 * 여기서 "만료된 알림"이란 요구사항 기준으로 확인한 지 1주일이 지난 알림을 의미합니다.
 * <p>
 * 요구사항: - 확인한 알림 중 1주일이 경과된 알림은 자동으로 삭제됩니다. - 삭제는 매일 배치로 수행합니다.
 * <p>
 * 실제 삭제 기준 계산과 DB 삭제는 NotificationAutoDeleteService가 담당합니다. 이 클래스는 매일 정해진 시간에 해당 서비스를 호출하는 책임만
 * 가집니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteExpiredNotificationJobConfig {

  private final NotificationAutoDeleteService notificationAutoDeleteService;

  /**
   * 매일 새벽 3시에 만료된 알림 삭제 작업을 실행합니다.
   * <p>
   * cron 표현식 "0 0 3 * * *"의 의미: - 초: 0 - 분: 0 - 시: 3 - 일: 매일 - 월: 매월 - 요일: 매 요일
   * <p>
   * 즉, 매일 03:00:00에 실행됩니다.
   */
  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  public void deleteExpiredNotifications() {
    // 배치는 HTTP 인터셉터의 대상이 아니므로 실행 시작을 직접 기록합니다.
    log.info("만료 알림 삭제 배치 시작: jobName=deleteExpiredNotifications");

    long startNanos = System.nanoTime();

    try {
      long deletedCount =
          notificationAutoDeleteService.deleteOldConfirmedNotifications();

      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
          System.nanoTime() - startNanos
      );

      // 완료 로그에는 실제 처리 건수와 소요시간을 포함합니다.
      log.info(
          "만료 알림 삭제 배치 완료: jobName=deleteExpiredNotifications, deletedCount={}, elapsedMs={}",
          deletedCount,
          elapsedMillis
      );
    } catch (RuntimeException exception) {
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
          System.nanoTime() - startNanos
      );

      // 스케줄러 예외는 공통 HTTP 예외 핸들러가 볼 수 없으므로
      // 발생 지점에서 스택트레이스와 함께 ERROR로 기록합니다.
      log.error(
          "만료 알림 삭제 배치 실패: jobName=deleteExpiredNotifications, elapsedMs={}",
          elapsedMillis,
          exception
      );

      // 배치 실패 사실이 Spring Scheduler에도 전달되도록 다시 던집니다.
      throw exception;
    }
  }
}