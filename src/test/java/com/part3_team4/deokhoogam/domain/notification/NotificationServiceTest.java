package com.part3_team4.deokhoogam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationAccessDeniedException;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationNotFoundException;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NotificationServiceImpl의 비즈니스 로직을 검증하는 단위 테스트입니다.
 *
 * 이 테스트는 Spring 컨테이너를 띄우지 않고 Mockito로 Repository를 대체합니다.
 * 그래서 DB 연결 없이 Service 계층의 순수한 동작만 빠르게 검증할 수 있습니다.
 *
 * TDD 흐름에서는 이 테스트를 먼저 작성하고,
 * 테스트가 실패하는 것을 확인한 뒤 main 코드를 구현합니다.
 */

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  /**
   * 테스트 대상 객체
   *
   * 아래에 선언된 Mock 객체들을 NotificationServiceImpl 생성자에 자동으로 주입해준다.
   */
  @InjectMocks
  private NotificationServiceImpl notificationService;

  /**
   * Service가 의존하는 Repository입니다.
   *
   * 실제 DB를 사용하는 대신 Mockito mock 객체로 대체합니다.
   * given(...).willReturn(...)으로 원하는 조회 결과를 미리 지정할 수 있고,
   * then(...).should()로 save 같은 메서드 호출 여부를 검증할 수 있습니다.
   */
  @Mock
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("알림을 생성하면 수신자, 리뷰, 리뷰 내용, 메시지, 미확인 상태가 저장된다")
  void createNotification() {
    // given
    // 알림 수신자 ID입니다.
    // 요구사항에서 "내가 작성한 리뷰에 좋아요 또는 댓글이 달리면 알림 생성"이므로,
    // userId는 리뷰 작성자, 즉 알림을 받을 사용자입니다.
    UUID userId = UUID.randomUUID();

    // 알림과 연결될 리뷰 ID입니다.
    UUID reviewId = UUID.randomUUID();

    // Swagger 응답 필드에 reviewContent가 있으므로,
    // 알림 생성 시점에 리뷰 내용을 함께 저장한다고 가정합니다.
    String reviewContent = "이 책은 인물의 감정선이 좋아요.";

    // 댓글/좋아요를 남긴 사용자 이름입니다.
    // 실제 메시지에 "우디님이 ..."처럼 포함되어야 합니다.
    String sender = "우디";

    // when
    // 알림 생성 메서드를 호출합니다.
    // 이 메서드는 Notification 엔티티를 만들고 repository.save(...)를 호출해야 합니다.
    notificationService.createNotification(userId, reviewId, reviewContent, sender);

    // then
    // repository.save(...)에 어떤 Notification 객체가 전달되었는지 확인하기 위해
    // ArgumentCaptor를 사용합니다.
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

    // save가 정확히 호출되었는지 검증하고, 저장 대상 객체를 캡처합니다.
    then(notificationRepository).should().save(captor.capture());

    Notification savedNotification = captor.getValue();

    // 저장될 알림의 수신자 ID가 알림을 받을 사용자와 같은지 검증합니다.
    assertThat(savedNotification.getUserId()).isEqualTo(userId);

    // 저장될 알림이 어떤 리뷰에 대한 알림인지 검증합니다.
    assertThat(savedNotification.getReviewId()).isEqualTo(reviewId);

    // 저장될 알림이 리뷰 내용을 가지고 있는지 검증합니다.
    assertThat(savedNotification.getReviewContent()).isEqualTo(reviewContent);

    // 메시지에는 최소한 행위자 이름이 포함되어야 합니다.
    // 문구 전체를 고정하면 나중에 문구 수정만으로 테스트가 깨질 수 있으므로
    // 여기서는 sender 포함 여부만 검증합니다.
    assertThat(savedNotification.getMessage()).contains(sender);

    // 새로 생성된 알림은 아직 사용자가 확인하지 않았으므로 false여야 합니다.
    assertThat(savedNotification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("본인 알림의 읽음 상태를 수정할 수 있다")
  void updateConfirmed() {
    // given
    // 알림의 소유자이자 요청자입니다.
    UUID userId = UUID.randomUUID();

    // 아직 읽지 않은 알림을 하나 준비합니다.
    // 이 테스트에서는 DB에서 조회된 엔티티라고 생각하면 됩니다.
    Notification notification = Notification.builder()
        .userId(userId)
        .reviewId(UUID.randomUUID())
        .reviewContent("좋은 리뷰입니다.")
        .message("버즈님이 내 리뷰에 댓글을 남겼습니다.")
        .confirmed(false)
        .build();

    // BaseEntity에서 생성된 알림 ID입니다.
    // Service는 이 ID로 알림을 조회해야 합니다.
    UUID notificationId = notification.getId();

    // repository.findById(notificationId)를 호출하면
    // 위에서 만든 notification이 조회된 것처럼 동작하게 설정합니다.
    given(notificationRepository.findById(notificationId))
        .willReturn(Optional.of(notification));

    // Swagger의 PATCH /api/notifications/{notificationId} 요청 body입니다.
    // { "confirmed": true }에 해당합니다.
    NotificationUpdateRequest request = new NotificationUpdateRequest(true);

    // when
    // 본인 알림의 읽음 상태를 true로 변경합니다.
    NotificationDto result = notificationService.updateConfirmed(notificationId, userId, request);

    // then
    // 실제 엔티티의 confirmed 값이 true로 변경되었는지 검증합니다.
    assertThat(notification.isConfirmed()).isTrue();

    // Service 응답 DTO가 Swagger 응답 형식에 맞는 값을 담는지 검증합니다.
    assertThat(result.id()).isEqualTo(notificationId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.confirmed()).isTrue();
  }

  @Test
  @DisplayName("다른 사용자의 알림 읽음 상태는 수정할 수 없다")
  void updateConfirmedForbidden() {
    // given
    // 알림의 실제 소유자입니다.
    UUID ownerId = UUID.randomUUID();

    // 요청자입니다.
    // ownerId와 다르므로 이 사용자는 해당 알림을 수정할 권한이 없습니다.
    UUID requesterId = UUID.randomUUID();

    // 다른 사용자의 알림을 준비합니다.
    Notification notification = Notification.builder()
        .userId(ownerId)
        .reviewId(UUID.randomUUID())
        .reviewContent("좋은 리뷰입니다.")
        .message("제시님이 내 리뷰에 좋아요를 눌렀습니다.")
        .confirmed(false)
        .build();

    UUID notificationId = notification.getId();

    // 알림 자체는 존재하는 상황입니다.
    // 따라서 404가 아니라 권한 없음 예외가 발생해야 합니다.
    given(notificationRepository.findById(notificationId))
        .willReturn(Optional.of(notification));

    NotificationUpdateRequest request = new NotificationUpdateRequest(true);

    // when & then
    // 요청자와 알림 소유자가 다르므로 권한 없음 예외가 발생해야 합니다.
    assertThatThrownBy(() ->
        notificationService.updateConfirmed(notificationId, requesterId, request)
    ).isInstanceOf(NotificationAccessDeniedException.class);

    // 권한 없는 요청이므로 알림 상태가 바뀌면 안 됩니다.
    assertThat(notification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("존재하지 않는 알림을 수정하면 예외가 발생한다")
  void updateConfirmedNotFound() {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    // 해당 ID의 알림이 DB에 없는 상황을 mock으로 표현합니다.
    given(notificationRepository.findById(notificationId))
        .willReturn(Optional.empty());

    NotificationUpdateRequest request = new NotificationUpdateRequest(true);

    // when & then
    // 알림이 존재하지 않으므로 NotificationNotFoundException이 발생해야 합니다.
    assertThatThrownBy(() ->
        notificationService.updateConfirmed(notificationId, requesterId, request)
    ).isInstanceOf(NotificationNotFoundException.class);
  }

  @Test
  @DisplayName("사용자의 모든 미확인 알림을 읽음 처리한다")
  void readAll() {
    // given
    UUID userId = UUID.randomUUID();

    // 같은 사용자의 미확인 알림 2개를 준비합니다.
    Notification first = Notification.builder()
        .userId(userId)
        .reviewId(UUID.randomUUID())
        .reviewContent("첫 번째 리뷰")
        .message("댓글 알림")
        .confirmed(false)
        .build();

    Notification second = Notification.builder()
        .userId(userId)
        .reviewId(UUID.randomUUID())
        .reviewContent("두 번째 리뷰")
        .message("좋아요 알림")
        .confirmed(false)
        .build();

    // 전체 읽음 처리에서는 미확인 알림만 조회하면 됩니다.
    // 이미 읽은 알림은 다시 처리할 필요가 없습니다.
    given(notificationRepository.findAllByUserIdAndConfirmedFalse(userId))
        .willReturn(List.of(first, second));

    // when
    // 사용자의 모든 미확인 알림을 읽음 처리합니다.
    notificationService.readAll(userId);

    // then
    // 조회된 모든 미확인 알림의 confirmed 값이 true로 바뀌어야 합니다.
    assertThat(first.isConfirmed()).isTrue();
    assertThat(second.isConfirmed()).isTrue();
  }

  @Test
  @DisplayName("내가 내 리뷰에 반응한 경우 알림을 생성하지 않는다")
  void createNotificationSkippedWhenSelfAction() {
    // given
    // 리뷰 작성자 ID입니다.
    UUID reviewOwnerId = UUID.randomUUID();

    // 행동한 사용자 ID입니다.
    // 여기서는 리뷰 작성자와 행동한 사용자가 같습니다.
    UUID actorId = reviewOwnerId;

    UUID reviewId = UUID.randomUUID();

    // when
    // 자기 자신의 리뷰에 본인이 좋아요/댓글을 남긴 상황입니다.
    // 이 경우 알림을 만들면 사용자에게 불필요한 자기 알림이 생깁니다.
    //
    // 이 테스트는 현재 createNotification 메서드 시그니처로는 통과할 수 없습니다.
    // 그래서 Green 단계에서 actorId를 받는 오버로드 또는 새 메서드가 필요하다는 것을 알려줍니다.
    notificationService.createNotification(
        reviewOwnerId,
        reviewId,
        "내가 작성한 리뷰입니다.",
        "우디",
        actorId
    );

    // then
    // 자기 행동이므로 repository.save(...)가 호출되면 안 됩니다.
    then(notificationRepository).should(never()).save(any(Notification.class));
  }

  @Test
  @DisplayName("좋아요 알림을 생성하면 좋아요 전용 메시지로 알림이 저장된다")
  void createLikeNotification() {
    // given
    // 알림을 받을 사용자 ID입니다.
    // 좋아요 알림에서는 리뷰 작성자가 수신자가 됩니다.
    UUID receiverId = UUID.randomUUID();

    // 좋아요가 눌린 리뷰 ID입니다.
    UUID reviewId = UUID.randomUUID();

    // 알림에 함께 저장할 리뷰 내용입니다.
    // 리뷰가 나중에 수정되거나 삭제되더라도 알림에는 당시 내용을 보여줄 수 있습니다.
    String reviewContent = "이 책은 결말이 특히 인상적이었습니다.";

    // 좋아요를 누른 사용자 닉네임입니다.
    // 메시지에는 이 값이 포함되어야 합니다.
    String sender = "버즈";

    // 좋아요를 누른 사용자 ID입니다.
    // 자기 자신의 리뷰에 좋아요를 누른 경우 알림 생성을 막기 위해 사용합니다.
    UUID actorId = UUID.randomUUID();

    // when
    // 좋아요 이벤트가 발생했을 때 리뷰/좋아요 도메인이 호출할 알림 생성 메서드입니다.
    //
    // 현재는 이 메서드가 아직 없으므로 Red 단계에서 컴파일 실패가 나는 것이 정상입니다.
    notificationService.createLikeNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender,
        actorId
    );

    // then
    // 저장되는 Notification 객체를 캡처해서 필드와 메시지를 검증합니다.
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

    then(notificationRepository)
        .should()
        .save(captor.capture());

    Notification savedNotification = captor.getValue();

    // 수신자는 리뷰 작성자여야 합니다.
    assertThat(savedNotification.getUserId()).isEqualTo(receiverId);

    // 알림은 좋아요가 발생한 리뷰와 연결되어야 합니다.
    assertThat(savedNotification.getReviewId()).isEqualTo(reviewId);

    // 알림에는 리뷰 내용이 함께 저장되어야 합니다.
    assertThat(savedNotification.getReviewContent()).isEqualTo(reviewContent);

    // 좋아요 알림은 좋아요 전용 메시지를 가져야 합니다.
    assertThat(savedNotification.getMessage())
        .isEqualTo("버즈님이 내 리뷰에 좋아요를 눌렀습니다.");

    // 새로 생성된 알림은 아직 확인하지 않은 상태여야 합니다.
    assertThat(savedNotification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("댓글 알림을 생성하면 댓글 전용 메시지로 알림이 저장된다")
  void createCommentNotification() {
    // given
    // 알림을 받을 사용자 ID입니다.
    // 댓글 알림에서는 리뷰 작성자가 수신자가 됩니다.
    UUID receiverId = UUID.randomUUID();

    // 댓글이 달린 리뷰 ID입니다.
    UUID reviewId = UUID.randomUUID();

    // 알림에 함께 저장할 리뷰 내용입니다.
    String reviewContent = "문장마다 여운이 남는 리뷰입니다.";

    // 댓글을 작성한 사용자 닉네임입니다.
    String sender = "제시";

    // 댓글을 작성한 사용자 ID입니다.
    UUID actorId = UUID.randomUUID();

    // when
    // 댓글 이벤트가 발생했을 때 댓글 도메인이 호출할 알림 생성 메서드입니다.
    //
    // 현재는 이 메서드가 아직 없으므로 Red 단계에서 컴파일 실패가 나는 것이 정상입니다.
    notificationService.createCommentNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender,
        actorId
    );

    // then
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

    then(notificationRepository)
        .should()
        .save(captor.capture());

    Notification savedNotification = captor.getValue();

    // 수신자는 리뷰 작성자여야 합니다.
    assertThat(savedNotification.getUserId()).isEqualTo(receiverId);

    // 알림은 댓글이 달린 리뷰와 연결되어야 합니다.
    assertThat(savedNotification.getReviewId()).isEqualTo(reviewId);

    // 알림에는 리뷰 내용이 함께 저장되어야 합니다.
    assertThat(savedNotification.getReviewContent()).isEqualTo(reviewContent);

    // 댓글 알림은 댓글 전용 메시지를 가져야 합니다.
    assertThat(savedNotification.getMessage())
        .isEqualTo("제시님이 내 리뷰에 댓글을 남겼습니다.");

    // 새로 생성된 알림은 아직 확인하지 않은 상태여야 합니다.
    assertThat(savedNotification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("리뷰가 기간별 인기 리뷰 Top 10에 선정되면 작성자에게 알림을 생성한다")
  void createPopularReviewNotification() {
    // given
    // 인기 리뷰 알림을 받을 리뷰 작성자 ID입니다.
    UUID receiverId = UUID.randomUUID();

    // 인기 순위에 선정된 리뷰 ID입니다.
    UUID reviewId = UUID.randomUUID();

    // 알림 화면에 함께 표시할 리뷰 내용입니다.
    String reviewContent = "등장인물의 감정 변화가 인상적인 책이었습니다.";

    // 현재 PopularReview 엔티티가 기간을 String으로 관리하므로
    // 알림 Service도 동일하게 String 값을 전달받습니다.
    String period = "DAILY";

    // 해당 기간의 인기 리뷰 순위입니다.
    // 1위부터 10위까지만 알림 생성 대상입니다.
    int rank = 3;

    // when
    // 인기 리뷰 집계 배치가 Top 10 결과를 확정한 뒤 호출할 메서드입니다.
    //
    // 아직 createPopularReviewNotification(...) 메서드가 없으므로
    // 현재 Red 단계에서는 컴파일 실패가 발생하는 것이 정상입니다.
    notificationService.createPopularReviewNotification(
        receiverId,
        reviewId,
        reviewContent,
        period,
        rank
    );

    // then
    // Repository에 전달된 Notification을 캡처하여
    // 수신자, 리뷰 정보, 메시지, 확인 상태를 검증합니다.
    ArgumentCaptor<Notification> captor =
        ArgumentCaptor.forClass(Notification.class);

    then(notificationRepository)
        .should()
        .save(captor.capture());

    Notification savedNotification = captor.getValue();

    // 인기 리뷰 작성자가 알림 수신자여야 합니다.
    assertThat(savedNotification.getUserId()).isEqualTo(receiverId);

    // 선정된 인기 리뷰와 알림이 연결되어야 합니다.
    assertThat(savedNotification.getReviewId()).isEqualTo(reviewId);

    // 알림에는 선정 당시 리뷰 내용이 함께 저장되어야 합니다.
    assertThat(savedNotification.getReviewContent()).isEqualTo(reviewContent);

    // DAILY는 사용자에게 보여줄 때 "일간"으로 표현하고,
    // 몇 위에 선정되었는지 메시지에 포함해야 합니다.
    assertThat(savedNotification.getMessage())
        .isEqualTo("내 리뷰가 일간 인기 리뷰 3위에 선정되었습니다.");

    // 새로 생성된 알림은 아직 확인하지 않은 상태여야 합니다.
    assertThat(savedNotification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("리뷰 순위가 Top 10 밖이면 인기 리뷰 알림을 생성하지 않는다")
  void createPopularReviewNotificationSkippedWhenOutsideTop10() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();

    String reviewContent = "인기 리뷰 순위 테스트를 위한 내용입니다.";
    String period = "WEEKLY";

    // 11위는 Top 10 범위에 포함되지 않으므로 알림 생성 대상이 아닙니다.
    int rank = 11;

    // when
    notificationService.createPopularReviewNotification(
        receiverId,
        reviewId,
        reviewContent,
        period,
        rank
    );

    // then
    // 요구사항은 각 기간별 10위 이내에 선정된 리뷰만 대상으로 하므로
    // 11위 리뷰에 대한 Notification이 저장되면 안 됩니다.
    then(notificationRepository)
        .should(never())
        .save(any(Notification.class));
  }

}