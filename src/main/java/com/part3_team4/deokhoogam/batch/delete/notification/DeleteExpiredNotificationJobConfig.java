package com.part3_team4.deokhoogam.batch.delete.notification;

import com.part3_team4.deokhoogam.domain.notification.service.NotificationAutoDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 알림을 주기적으로 삭제하는 배치 실행 클래스입니다.
 *
 * 여기서 "만료된 알림"이란 요구사항 기준으로
 * 확인한 지 1주일이 지난 알림을 의미합니다.
 *
 * 요구사항:
 * - 확인한 알림 중 1주일이 경과된 알림은 자동으로 삭제됩니다.
 * - 삭제는 매일 배치로 수행합니다.
 *
 * 실제 삭제 기준 계산과 DB 삭제는 NotificationAutoDeleteService가 담당합니다.
 * 이 클래스는 매일 정해진 시간에 해당 서비스를 호출하는 책임만 가집니다.
 */
@Component
@RequiredArgsConstructor
public class DeleteExpiredNotificationJobConfig {

  private final NotificationAutoDeleteService notificationAutoDeleteService;

  /**
   * 매일 새벽 3시에 만료된 알림 삭제 작업을 실행합니다.
   *
   * cron 표현식 "0 0 3 * * *"의 의미:
   * - 초: 0
   * - 분: 0
   * - 시: 3
   * - 일: 매일
   * - 월: 매월
   * - 요일: 매 요일
   *
   * 즉, 매일 03:00:00에 실행됩니다.
   */
  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  public void deleteExpiredNotifications() {
    notificationAutoDeleteService.deleteOldConfirmedNotifications();
  }
}