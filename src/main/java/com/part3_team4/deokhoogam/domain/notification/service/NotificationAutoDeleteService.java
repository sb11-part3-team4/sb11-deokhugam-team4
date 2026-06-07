package com.part3_team4.deokhoogam.domain.notification.service;

import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 확인 후 오래 지난 알림을 자동 삭제하는 Service입니다.
 *
 * 요구사항:
 * - 확인한 알림 중 1주일이 경과된 알림은 자동으로 삭제됩니다.
 * - 삭제는 매일 배치로 수행합니다.
 *
 * 이 Service는 실제 삭제 실행 조건을 계산합니다.
 * Scheduler 또는 Batch는 매일 이 Service를 호출하기만 하고,
 * "어떤 알림을 삭제할지"에 대한 기준은 이 Service가 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class NotificationAutoDeleteService {

  private final NotificationRepository notificationRepository;

  /**
   * 현재 시각을 가져오기 위한 Clock입니다.
   *
   * Instant.now()를 직접 사용하면 테스트에서 현재 시각을 고정하기 어렵습니다.
   * Clock을 주입받으면 테스트에서는 고정된 시간을 사용할 수 있고,
   * 운영에서는 시스템 시간을 사용할 수 있습니다.
   */
  private final Clock clock;

  /**
   * 확인한 지 1주일이 지난 알림을 삭제합니다.
   *
   * 처리 흐름:
   * 1. 현재 시각을 구합니다.
   * 2. 현재 시각에서 7일을 뺀 값을 삭제 기준 시각으로 계산합니다.
   * 3. confirmed=true 이면서 updatedAt이 기준 시각보다 이전인 알림을 삭제합니다.
   *
   * @return 삭제된 알림 개수
   */
  @Transactional
  public long deleteOldConfirmedNotifications() {
    Instant threshold = clock.instant().minus(7, ChronoUnit.DAYS);

    return notificationRepository.deleteByConfirmedTrueAndUpdatedAtBefore(threshold);
  }
}