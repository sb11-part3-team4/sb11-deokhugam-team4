package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 알림 자동 삭제 테스트입니다.
 *
 * 요구사항:
 * - 확인한 알림 중 1주일이 경과된 알림은 자동으로 삭제됩니다.
 * - 삭제는 매일 배치로 수행합니다.
 *
 * 이번 테스트는 자동 삭제의 가장 핵심 조건을 검증합니다.
 * 즉, confirmed=true 이면서 updatedAt이 기준 시각보다 오래된 알림만 삭제되어야 합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NotificationAutoDeleteTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("확인 후 1주일이 지난 알림만 삭제한다")
  void deleteConfirmedNotificationsUpdatedBefore() {
    // given
    // 현재 시각을 기준으로 삭제 기준 시각을 계산합니다.
    // 실제 배치에서는 now - 7 days가 기준이 됩니다.
    Instant now = Instant.parse("2026-06-04T00:00:00Z");
    Instant threshold = now.minus(7, ChronoUnit.DAYS);

    // 알림을 확인했고, updatedAt이 기준 시각보다 이전입니다.
    // 요구사항상 삭제 대상입니다.
    Notification oldConfirmedNotification = createNotification(true);
    setAuditTime(
        oldConfirmedNotification,
        now.minus(8, ChronoUnit.DAYS)
    );

    // 알림을 확인했지만, updatedAt이 아직 기준 시각 이후입니다.
    // 1주일이 지나지 않았으므로 삭제되면 안 됩니다.
    Notification recentConfirmedNotification = createNotification(true);
    setAuditTime(
        recentConfirmedNotification,
        now.minus(3, ChronoUnit.DAYS)
    );

    // 알림을 확인하지 않은 알림입니다.
    // 오래되었더라도 confirmed=false 이므로 삭제되면 안 됩니다.
    Notification oldUnconfirmedNotification = createNotification(false);
    setAuditTime(
        oldUnconfirmedNotification,
        now.minus(10, ChronoUnit.DAYS)
    );

    notificationRepository.save(oldConfirmedNotification);
    notificationRepository.save(recentConfirmedNotification);
    notificationRepository.save(oldUnconfirmedNotification);
    notificationRepository.flush();

    // when
    // 기준 시각보다 updatedAt이 오래된 확인 알림을 삭제합니다.
    //
    // 이 메서드는 아직 Repository에 없으므로
    // 지금 테스트를 작성하면 Red 단계에서 컴파일 실패가 나는 것이 정상입니다.
    long deletedCount =
        notificationRepository.deleteByConfirmedTrueAndUpdatedAtBefore(threshold);

    // then
    // 삭제 대상은 oldConfirmedNotification 하나뿐입니다.
    assertThat(deletedCount).isEqualTo(1);

    // 삭제 대상 알림은 DB에서 사라져야 합니다.
    assertThat(notificationRepository.existsById(oldConfirmedNotification.getId()))
        .isFalse();

    // 확인했지만 1주일이 지나지 않은 알림은 남아 있어야 합니다.
    assertThat(notificationRepository.existsById(recentConfirmedNotification.getId()))
        .isTrue();

    // 확인하지 않은 알림은 오래되었더라도 남아 있어야 합니다.
    assertThat(notificationRepository.existsById(oldUnconfirmedNotification.getId()))
        .isTrue();
  }

  /**
   * 테스트용 알림 Entity를 생성합니다.
   *
   * 이번 테스트의 핵심은 삭제 조건인 confirmed와 updatedAt이므로,
   * userId, reviewId, reviewContent, message는 유효한 더미 값으로 채웁니다.
   */
  private Notification createNotification(boolean confirmed) {
    return Notification.builder()
        .userId(UUID.randomUUID())
        .reviewId(UUID.randomUUID())
        .reviewContent("리뷰 내용")
        .message("알림 메시지")
        .confirmed(confirmed)
        .build();
  }

  /**
   * BaseEntity의 createdAt, updatedAt 값을 테스트에서 직접 세팅합니다.
   *
   * 실제 운영 코드에서는 JPA Auditing이 createdAt, updatedAt을 자동으로 채웁니다.
   * 하지만 자동 삭제 테스트에서는 "며칠 전 읽음 처리된 알림"을 만들어야 하므로,
   * ReflectionTestUtils를 사용해 테스트 데이터의 시간을 명시적으로 조정합니다.
   *
   * updatedAt은 알림을 읽음 처리한 시각으로 보고,
   * 자동 삭제 기준 판단에 사용합니다.
   */
  private void setAuditTime(Notification notification, Instant time) {
    ReflectionTestUtils.setField(notification, "createdAt", time);
    ReflectionTestUtils.setField(notification, "updatedAt", time);
  }
}