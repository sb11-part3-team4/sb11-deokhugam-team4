package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationAutoDeleteService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 알림 자동 삭제 Service 테스트입니다.
 *
 * 요구사항:
 * - 확인한 알림 중 1주일이 경과된 알림은 자동으로 삭제됩니다.
 * - 삭제는 매일 배치로 수행합니다.
 *
 * Repository 테스트에서는 삭제 조건 자체를 검증했습니다.
 * 이 Service 테스트에서는 "현재 시각 기준 7일 전"을 계산해서
 * Repository 삭제 메서드에 전달하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationAutoDeleteServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  /**
   * 테스트에서 현재 시각을 고정하기 위한 Clock입니다.
   *
   * Instant.now()를 서비스 내부에서 직접 호출하면
   * 테스트 실행 시점에 따라 기준 시간이 계속 달라집니다.
   *
   * 따라서 Clock을 주입받도록 설계하면,
   * 테스트에서는 고정된 현재 시각을 사용하고
   * 운영에서는 실제 시스템 시간을 사용할 수 있습니다.
   */
  @Mock
  private Clock clock;

  @InjectMocks
  private NotificationAutoDeleteService notificationAutoDeleteService;

  @Test
  @DisplayName("현재 시각 기준 7일이 지난 확인 알림을 삭제한다")
  void deleteOldConfirmedNotifications() {
    // given
    // 테스트에서 사용할 고정 현재 시각입니다.
    Instant fixedNow = Instant.parse("2026-06-04T00:00:00Z");

    // 요구사항상 확인 후 1주일이 지난 알림을 삭제해야 하므로,
    // Service는 fixedNow - 7일 값을 threshold로 계산해야 합니다.
    Instant expectedThreshold = fixedNow.minus(7, ChronoUnit.DAYS);

    // Clock.instant()가 고정 현재 시각을 반환하도록 설정합니다.
    // Service는 이 값을 기준으로 7일 전 threshold를 계산합니다.
    given(clock.instant()).willReturn(fixedNow);

    // Repository가 삭제된 알림 개수 3개를 반환한다고 가정합니다.
    given(notificationRepository.deleteByConfirmedTrueAndUpdatedAtBefore(expectedThreshold))
        .willReturn(3L);

    // when
    // 오래된 확인 알림 삭제를 실행합니다.
    long deletedCount = notificationAutoDeleteService.deleteOldConfirmedNotifications();

    // then
    // Repository가 반환한 삭제 개수를 Service가 그대로 반환해야 합니다.
    assertThat(deletedCount).isEqualTo(3L);

    // Service가 Repository에 전달한 threshold 값을 캡처합니다.
    ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);

    then(notificationRepository)
        .should()
        .deleteByConfirmedTrueAndUpdatedAtBefore(thresholdCaptor.capture());

    // 캡처한 threshold가 현재 시각 기준 7일 전인지 검증합니다.
    assertThat(thresholdCaptor.getValue()).isEqualTo(expectedThreshold);
  }
}