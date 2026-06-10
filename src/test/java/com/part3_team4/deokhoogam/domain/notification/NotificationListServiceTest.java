package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationServiceImpl;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationInvalidInputException;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationAccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;


/**
 * 알림 목록 조회 Service 로직을 검증하는 테스트입니다.
 *
 * Repository 테스트에서는 DB 쿼리가 올바르게 동작하는지 검증했습니다.
 * 이 테스트에서는 Repository가 반환한 알림 목록을 Service가
 * Swagger 응답 형식에 맞는 PageResponse로 조립하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationListServiceTest {

  /**
   * 테스트 대상 Service 구현체입니다.
   *
   * Repository는 mock 객체로 대체하고,
   * Service의 페이지 응답 조립 로직만 검증합니다.
   */
  @InjectMocks
  private NotificationServiceImpl notificationService;

  /**
   * 알림 Repository mock입니다.
   *
   * 실제 DB 조회는 RepositoryTest에서 검증했기 때문에,
   * 여기서는 원하는 조회 결과를 미리 지정해 Service 동작만 확인합니다.
   */
  @Mock
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("커서가 없으면 첫 페이지를 조회하고 limit 기준으로 다음 커서를 생성한다")
  void findAllFirstPage() {
    // given
    UUID userId = UUID.randomUUID();
    int limit = 2;

    Notification newest = createNotification(
        userId,
        "가장 최신 알림",
        Instant.parse("2026-06-01T03:00:00Z")
    );

    Notification second = createNotification(
        userId,
        "두 번째 알림",
        Instant.parse("2026-06-01T02:00:00Z")
    );

    Notification third = createNotification(
        userId,
        "세 번째 알림",
        Instant.parse("2026-06-01T01:00:00Z")
    );

    // Service는 hasNext 판단을 위해 limit + 1개를 조회해야 합니다.
    given(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
        userId,
        PageRequest.of(0, limit + 1)
    )).willReturn(List.of(newest, second, third));

    given(notificationRepository.countByUserId(userId))
        .willReturn(3L);

    // when
    PageResponse<NotificationDto> result = notificationService.findAll(
        userId,
        userId,
        Sort.Direction.DESC,
        null,
        null,
        limit
    );

    // then
    // Repository는 3개를 반환했지만, 응답 content에는 limit인 2개만 담겨야 합니다.
    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).id()).isEqualTo(newest.getId());
    assertThat(result.content().get(1).id()).isEqualTo(second.getId());

    // limit + 1개가 조회되었으므로 다음 페이지가 있다고 판단해야 합니다.
    assertThat(result.hasNext()).isTrue();

    // 다음 커서는 응답 content의 마지막 요소 기준으로 만들어야 합니다.
    assertThat(result.nextCursor()).isEqualTo(second.getId().toString());
    assertThat(result.nextAfter()).isEqualTo(second.getCreatedAt().toString());

    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3L);
  }

  @Test
  @DisplayName("커서가 있으면 커서 다음 페이지를 조회하고 다음 페이지가 없으면 커서를 비운다")
  void findAllNextPageWithoutHasNext() {
    // given
    UUID userId = UUID.randomUUID();
    UUID cursor = UUID.randomUUID();
    Instant after = Instant.parse("2026-06-01T02:00:00Z");
    int limit = 2;

    Notification oldest = createNotification(
        userId,
        "마지막 알림",
        Instant.parse("2026-06-01T01:00:00Z")
    );

    given(notificationRepository.findNextPageDesc(
        userId,
        after,
        cursor,
        PageRequest.of(0, limit + 1)
    )).willReturn(List.of(oldest));

    given(notificationRepository.countByUserId(userId))
        .willReturn(3L);

    // when
    PageResponse<NotificationDto> result = notificationService.findAll(
        userId,
        userId,
        Sort.Direction.DESC,
        cursor,
        after,
        limit
    );

    // then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).id()).isEqualTo(oldest.getId());

    // limit + 1개보다 적게 조회되었으므로 다음 페이지가 없어야 합니다.
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextAfter()).isNull();

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.totalElements()).isEqualTo(3L);
  }

  /**
   * 테스트용 Notification을 생성합니다.
   *
   * Service 단위 테스트에서는 JPA Auditing이 동작하지 않으므로,
   * ReflectionTestUtils로 createdAt 값을 직접 주입합니다.
   */
  private Notification createNotification(UUID userId, String message, Instant createdAt) {
    Notification notification = Notification.builder()
        .userId(userId)
        .reviewId(UUID.randomUUID())
        .reviewContent("리뷰 내용")
        .message(message)
        .confirmed(false)
        .build();

    ReflectionTestUtils.setField(notification, "createdAt", createdAt);
    ReflectionTestUtils.setField(notification, "updatedAt", createdAt);

    return notification;
  }

  @Test
  @DisplayName("cursor만 전달하면 잘못된 페이지네이션 요청 예외가 발생한다")
  void findAllWithCursorOnly() {
    // given
    UUID userId = UUID.randomUUID();
    UUID cursor = UUID.randomUUID();

    // when & then
    // 다음 페이지 조회에는 cursor와 after가 모두 필요합니다.
    // cursor만 전달된 요청을 첫 페이지로 처리하면 클라이언트가
    // 같은 목록을 반복해서 조회할 수 있으므로 400 예외를 발생시킵니다.
    assertThatThrownBy(() ->
        notificationService.findAll(
            userId,
            userId,
            Sort.Direction.DESC,
            cursor,
            null,
            20
        )
    ).isInstanceOf(NotificationInvalidInputException.class);

    // 입력값 검증에서 요청이 중단되어야 하므로
    // Repository 조회는 한 번도 실행되면 안 됩니다.
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("after만 전달하면 잘못된 페이지네이션 요청 예외가 발생한다")
  void findAllWithAfterOnly() {
    // given
    UUID userId = UUID.randomUUID();
    Instant after = Instant.parse("2026-06-01T03:00:00Z");

    // when & then
    // 생성 시각만으로는 동일한 시각에 생성된 알림을 구분할 수 없습니다.
    // 보조 정렬 조건인 cursor가 없으므로 잘못된 요청으로 처리합니다.
    assertThatThrownBy(() ->
        notificationService.findAll(
            userId,
            userId,
            Sort.Direction.DESC,
            null,
            after,
            20
        )
    ).isInstanceOf(NotificationInvalidInputException.class);

    // 잘못된 요청이므로 DB 조회가 발생하지 않아야 합니다.
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("요청자와 조회 대상 사용자가 다르면 알림 목록을 조회할 수 없다")
  void findAllAccessDenied() {
    UUID requesterId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() ->
        notificationService.findAll(
            requesterId,
            userId,
            Sort.Direction.DESC,
            null,
            null,
            20
        )
    ).isInstanceOf(NotificationAccessDeniedException.class);
  }
}