package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import org.springframework.test.context.ActiveProfiles;

/**
 * NotificationRepository의 DB 조회 로직을 검증하는 테스트입니다.
 *
 * 이번 테스트의 핵심은 알림 목록 조회에 필요한 커서 페이지네이션 쿼리입니다.
 *
 * 알림 목록 조회 요구사항:
 * - 사용자별 알림 목록 조회
 * - 최근 시간 순 정렬
 * - 커서 페이지네이션
 *
 * 정렬 기준:
 * 1. createdAt DESC
 * 2. id DESC
 *
 * createdAt만으로 정렬하면 같은 시간에 생성된 데이터의 순서가 불안정할 수 있으므로,
 * id를 보조 정렬 기준으로 사용합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

  /**
   * 테스트 대상 Repository입니다.
   *
   * @DataJpaTest는 JPA 관련 Bean만 로드하고,
   * 테스트 DB로 Repository 동작을 검증할 수 있게 해줍니다.
   */
  @Autowired
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("커서가 없으면 사용자의 알림을 최신순으로 조회한다")
  void findFirstPageOrderByCreatedAtDesc() throws Exception {
    // given
    // 알림을 조회할 사용자입니다.
    UUID userId = UUID.randomUUID();

    // 다른 사용자의 알림입니다.
    // 목록 조회 결과에 섞이면 안 됩니다.
    UUID otherUserId = UUID.randomUUID();

    Notification first = notificationRepository.saveAndFlush(
        createNotification(userId, "첫 번째 알림"));

    // createdAt 차이를 확실하게 만들기 위한 짧은 대기입니다.
    // Repository 테스트에서 최신순 정렬을 검증하기 위해 사용합니다.
    Thread.sleep(5);

    Notification second = notificationRepository.saveAndFlush(
        createNotification(userId, "두 번째 알림"));

    Thread.sleep(5);

    Notification third = notificationRepository.saveAndFlush(
        createNotification(userId, "세 번째 알림"));

    // 다른 사용자의 알림도 저장합니다.
    // 조회 조건이 userId를 제대로 거는지 확인하기 위한 데이터입니다.
    notificationRepository.saveAndFlush(createNotification(otherUserId, "다른 사용자 알림"));

    // when
    // 첫 페이지 조회입니다.
    // limit + 1 전략을 사용할 예정이므로, Repository는 Pageable의 size만큼 조회하면 됩니다.
    List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
        userId,
        PageRequest.of(0, 2)
    );

    // then
    // limit이 2이므로 2개만 조회되어야 합니다.
    assertThat(result).hasSize(2);

    // 최신순이므로 나중에 저장한 third가 먼저 나와야 합니다.
    assertThat(result.get(0).getId()).isEqualTo(third.getId());
    assertThat(result.get(1).getId()).isEqualTo(second.getId());

    // 다른 사용자의 알림은 포함되면 안 됩니다.
    assertThat(result)
        .extracting(Notification::getUserId)
        .containsOnly(userId);
  }

  @Test
  @DisplayName("커서가 있으면 커서 다음 알림 목록을 최신순으로 조회한다")
  void findNextPageByCursorOrderByCreatedAtDesc() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    Notification first = notificationRepository.saveAndFlush(
        createNotification(userId, "첫 번째 알림")
    );

    Thread.sleep(5);

    Notification second = notificationRepository.saveAndFlush(
        createNotification(userId, "두 번째 알림")
    );

    Thread.sleep(5);

    Notification third = notificationRepository.saveAndFlush(
        createNotification(userId, "세 번째 알림")
    );

    // 최신순 전체 순서는 third -> second -> first 입니다.
    //
    // 첫 페이지에서 third, second를 받았다고 가정하면,
    // 다음 페이지의 커서는 second가 됩니다.
    UUID cursor = second.getId();
    Instant after = second.getCreatedAt();

    // when
    // 커서 이후의 다음 페이지를 조회합니다.
    List<Notification> result = notificationRepository.findNextPageDesc(
        userId,
        after,
        cursor,
        PageRequest.of(0, 2)
    );

    // then
    // second보다 오래된 first만 조회되어야 합니다.
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(first.getId());

    // 조회 결과는 해당 사용자 알림만 포함해야 합니다.
    assertThat(result)
        .extracting(Notification::getUserId)
        .containsOnly(userId);
  }

  /**
   * 테스트용 Notification Entity를 생성하는 helper 메서드입니다.
   *
   * 테스트마다 builder 코드를 반복하지 않기 위해 분리했습니다.
   * createdAt, updatedAt은 BaseEntity의 JPA Auditing이 저장 시점에 자동으로 채웁니다.
   */
  private Notification createNotification(UUID userId, String message) {
    return Notification.builder()
        .userId(userId)
        .reviewId(UUID.randomUUID())
        .reviewContent("리뷰 내용")
        .message(message)
        .confirmed(false)
        .build();
  }
}