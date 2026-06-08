package com.part3_team4.deokhoogam.batch.delete.notification;

import static org.mockito.BDDMockito.then;

import com.part3_team4.deokhoogam.domain.notification.service.NotificationAutoDeleteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 만료된 알림 삭제 배치 실행 클래스 테스트입니다.
 *
 * 실제 cron이 매일 실행되는지는 Spring Scheduler의 책임입니다.
 * 이 테스트에서는 스케줄러 메서드가 호출되었을 때
 * 알림 자동 삭제 Service로 작업을 위임하는지만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class DeleteExpiredNotificationJobConfigTest {

  @Mock
  private NotificationAutoDeleteService notificationAutoDeleteService;

  @InjectMocks
  private DeleteExpiredNotificationJobConfig deleteExpiredNotificationJobConfig;

  @Test
  @DisplayName("만료된 알림 삭제 배치가 실행되면 알림 자동 삭제 서비스를 호출한다")
  void deleteExpiredNotifications() {
    // when
    // 실제 운영에서는 @Scheduled에 의해 매일 새벽 3시에 호출됩니다.
    // 단위 테스트에서는 메서드를 직접 호출해서 내부 위임 로직만 검증합니다.
    deleteExpiredNotificationJobConfig.deleteExpiredNotifications();

    // then
    // 배치 실행 클래스는 삭제 기준을 직접 계산하지 않습니다.
    // 기준 계산과 실제 삭제는 NotificationAutoDeleteService에 위임합니다.
    then(notificationAutoDeleteService)
        .should()
        .deleteOldConfirmedNotifications();
  }
}