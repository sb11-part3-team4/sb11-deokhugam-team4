package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

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
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class NotificationAutoDeleteTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("확인 후 1주일이 지난 알림만 삭제한다")
  void deleteConfirmedNotificationsUpdatedBefore() {
    // given
    // 현재 시각을 기준으로 삭제 기준 시각을 계산합니다.
    // 실제 배치에서는 now - 7 days가 기준이 됩니다.
    Instant now = Instant.parse("2026-06-04T00:00:00Z");
    Instant threshold = now.minus(7, ChronoUnit.DAYS);

    // 알림을 확인했고, updatedAt이 기준 시각보다 이전입니다.
    // 이 알림만 삭제 대상이어야 합니다.
    Notification oldConfirmedNotification = notificationRepository.saveAndFlush(
        createNotification(true)
    );

    // 알림을 확인했지만, updatedAt이 아직 기준 시각 이후입니다.
    // 1주일이 지나지 않았으므로 삭제되면 안 됩니다.
    Notification recentConfirmedNotification = notificationRepository.saveAndFlush(
        createNotification(true)
    );

    // 알림을 확인하지 않은 알림입니다.
    // 오래되었더라도 confirmed=false 이므로 삭제되면 안 됩니다.
    Notification oldUnconfirmedNotification = notificationRepository.saveAndFlush(
        createNotification(false)
    );

    // save 시점에는 JPA Auditing이 createdAt, updatedAt을 현재 시각으로 자동 설정합니다.
    // 따라서 "8일 전 알림", "3일 전 알림" 같은 테스트 상황을 만들려면
    // 저장 이후 DB 값을 직접 갱신해야 합니다.
    //
    // bulk update는 엔티티 생명주기 콜백과 Auditing을 거치지 않으므로,
    // 테스트에서 원하는 updatedAt 값을 정확히 만들 수 있습니다.
    updateAuditTime(
        oldConfirmedNotification.getId(),
        now.minus(8, ChronoUnit.DAYS)
    );

    updateAuditTime(
        recentConfirmedNotification.getId(),
        now.minus(3, ChronoUnit.DAYS)
    );

    updateAuditTime(
        oldUnconfirmedNotification.getId(),
        now.minus(10, ChronoUnit.DAYS)
    );

    // bulk update 이후 영속성 컨텍스트에는 이전 엔티티 상태가 남아 있을 수 있습니다.
    // 이후 existsById, delete 쿼리가 DB 기준으로 동작하도록 flush/clear 합니다.
    entityManager.flush();
    entityManager.clear();

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
   * 테스트 데이터의 createdAt, updatedAt을 DB에서 직접 수정합니다.
   *
   * Notification은 BaseEntity의 JPA Auditing을 사용하므로,
   * save 시점에 createdAt과 updatedAt이 현재 시각으로 자동 설정됩니다.
   *
   * 자동 삭제 테스트에서는 "8일 전에 확인한 알림"처럼
   * 과거 updatedAt을 가진 데이터를 만들어야 하므로,
   * 저장 이후 bulk update로 DB 값을 직접 변경합니다.
   *
   * bulk update는 JPA Auditing을 다시 태우지 않기 때문에
   * 테스트에서 원하는 시간을 정확히 유지할 수 있습니다.
   */
  private void updateAuditTime(UUID notificationId, Instant time) {
    entityManager.getEntityManager()
        .createQuery("""
          update Notification n
          set n.createdAt = :time,
              n.updatedAt = :time
          where n.id = :notificationId
          """)
        .setParameter("time", time)
        .setParameter("notificationId", notificationId)
        .executeUpdate();
  }
}