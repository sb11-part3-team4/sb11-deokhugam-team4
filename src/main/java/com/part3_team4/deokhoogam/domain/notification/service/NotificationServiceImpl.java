package com.part3_team4.deokhoogam.domain.notification.service;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationAccessDeniedException;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationNotFoundException;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 알림 도메인의 실제 비즈니스 로직을 처리하는 Service 구현체입니다.
 *
 * 이 클래스는 NotificationServiceTest를 통과시키기 위한 Green 단계 구현입니다.
 * 따라서 현재는 서비스 테스트에서 요구한 기능만 최소한으로 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

  /**
   * 알림 Entity를 저장하거나 조회하기 위해 사용하는 Repository입니다.
   */
  private final NotificationRepository notificationRepository;

  /**
   * 알림을 생성합니다.
   *
   * 이 메서드는 알림을 받을 사용자, 연결된 리뷰, 리뷰 내용, 발신자 이름을 받아
   * Notification Entity를 생성한 뒤 저장합니다.
   *
   * 새 알림은 아직 확인하지 않은 상태이므로 confirmed=false로 저장합니다.
   */
  @Override
  public void createNotification(UUID userId, UUID reviewId, String reviewContent, String sender) {
    Notification notification = Notification.builder()
        .userId(userId)
        .reviewId(reviewId)
        .reviewContent(reviewContent)
        .message(sender + "님이 내 리뷰에 반응했습니다.")
        .confirmed(false)
        .build();

    notificationRepository.save(notification);
  }

  /**
   * actorId를 포함해서 알림을 생성합니다.
   *
   * 자기 행동 알림 방지를 위해 receiverId와 actorId를 비교합니다.
   *
   * receiverId:
   * - 알림을 받을 사용자
   *
   * actorId:
   * - 좋아요 또는 댓글 등 알림을 발생시킨 사용자
   *
   * 두 값이 같으면 "내가 내 리뷰에 반응한 상황"이므로 알림을 생성하지 않습니다.
   */
  @Override
  public void createNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  ) {
    if (receiverId.equals(actorId)) {
      return;
    }

    createNotification(receiverId, reviewId, reviewContent, sender);
  }

  /**
   * 특정 알림의 읽음 상태를 수정합니다.
   *
   * 처리 흐름:
   * 1. notificationId로 알림을 조회합니다.
   * 2. 알림이 없으면 NotificationNotFoundException을 던집니다.
   * 3. 알림의 userId와 요청자 ID가 다르면 권한 예외를 던집니다.
   * 4. 요청 body의 confirmed 값으로 알림 상태를 변경합니다.
   * 5. 변경된 알림을 NotificationDto로 변환해서 반환합니다.
   */
  @Override
  public NotificationDto updateConfirmed(
      UUID notificationId,
      UUID requesterId,
      NotificationUpdateRequest request
  ) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> NotificationNotFoundException.withId(notificationId));

    if (!notification.getUserId().equals(requesterId)) {
      throw NotificationAccessDeniedException.withId(notificationId);
    }

    notification.updateConfirmed(request.confirmed());

    return NotificationDto.from(notification);
  }

  /**
   * 요청자의 모든 미확인 알림을 읽음 처리합니다.
   *
   * 처리 흐름:
   * 1. requesterId의 미확인 알림만 조회합니다.
   * 2. 조회된 알림들의 confirmed 값을 true로 변경합니다.
   *
   * @Transactional이 적용되어 있으므로,
   * 조회된 Entity의 값을 변경하면 트랜잭션 커밋 시점에 DB에 반영됩니다.
   */
  @Override
  public void readAll(UUID requesterId) {
    List<Notification> notifications =
        notificationRepository.findAllByUserIdAndConfirmedFalse(requesterId);

    notifications.forEach(notification -> notification.updateConfirmed(true));
  }

  /**
   * 좋아요 발생 알림을 생성합니다.
   *
   * receiverId와 actorId가 같으면 자기 자신의 리뷰에 좋아요를 누른 상황이므로
   * 불필요한 자기 알림을 생성하지 않습니다.
   */
  @Override
  public void createLikeNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  ) {
    // 리뷰 작성자와 좋아요를 누른 사람이 같으면 알림을 저장하지 않습니다.
    if (receiverId.equals(actorId)) {
      return;
    }

    // 좋아요 알림에 맞는 메시지와 함께 Notification 엔티티를 저장합니다.
    saveNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender + "님이 내 리뷰에 좋아요를 눌렀습니다."
    );
  }

  /**
   * 댓글 발생 알림을 생성합니다.
   *
   * receiverId와 actorId가 같으면 자기 자신의 리뷰에 댓글을 작성한 상황이므로
   * 불필요한 자기 알림을 생성하지 않습니다.
   */
  @Override
  public void createCommentNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  ) {
    // 리뷰 작성자와 댓글 작성자가 같으면 알림을 저장하지 않습니다.
    if (receiverId.equals(actorId)) {
      return;
    }

    // 댓글 알림에 맞는 메시지와 함께 Notification 엔티티를 저장합니다.
    saveNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender + "님이 내 리뷰에 댓글을 남겼습니다."
    );
  }

  /**
   * 알림 유형별 메서드에서 공통으로 사용하는 저장 메서드입니다.
   *
   * 좋아요와 댓글 알림은 메시지만 다르고 Notification 생성 과정은 같으므로,
   * 중복되는 엔티티 생성 및 Repository 저장 코드를 이 메서드로 분리합니다.
   */
  private void saveNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String message
  ) {
    Notification notification = Notification.builder()
        .userId(receiverId)
        .reviewId(reviewId)
        .reviewContent(reviewContent)
        .message(message)
        // 새로 생성된 알림은 아직 사용자가 확인하지 않았으므로 false입니다.
        .confirmed(false)
        .build();

    notificationRepository.save(notification);
  }



  @Override
  @Transactional(readOnly = true)
  public PageResponse<NotificationDto> findAll(
      UUID requesterId,
      UUID userId,
      Sort.Direction direction,
      UUID cursor,
      Instant after,
      int limit
  ) {
    if (!requesterId.equals(userId)) {
      throw NotificationAccessDeniedException.withUserId(userId);
    }
    // hasNext 판단을 위해 요청 limit보다 1개 더 조회합니다.
    PageRequest pageRequest = PageRequest.of(0, limit + 1);

    List<Notification> notifications;

    // cursor와 after가 없으면 첫 페이지 조회입니다.
    if (cursor == null || after == null) {
      notifications = direction == Sort.Direction.ASC
          ? notificationRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId, pageRequest)
          : notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageRequest);
    } else {
      // cursor와 after가 있으면 다음 페이지 조회입니다.
      notifications = direction == Sort.Direction.ASC
          ? notificationRepository.findNextPageAsc(userId, after, cursor, pageRequest)
          : notificationRepository.findNextPageDesc(userId, after, cursor, pageRequest);
    }

    // limit + 1개가 조회되었다면 다음 페이지가 존재합니다.
    boolean hasNext = notifications.size() > limit;

    // 응답 content에는 실제 요청 limit 개수만 포함합니다.
    List<Notification> pageContent = hasNext
        ? notifications.subList(0, limit)
        : notifications;

    List<NotificationDto> content = pageContent.stream()
        .map(NotificationDto::from)
        .toList();

    String nextCursor = null;
    String nextAfter = null;

    // 다음 페이지가 있을 때만 다음 커서 값을 내려줍니다.
    if (hasNext && !pageContent.isEmpty()) {
      Notification last = pageContent.get(pageContent.size() - 1);
      nextCursor = last.getId().toString();
      nextAfter = last.getCreatedAt().toString();
    }

    return new PageResponse<>(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        notificationRepository.countByUserId(userId),
        hasNext
    );
  }
}