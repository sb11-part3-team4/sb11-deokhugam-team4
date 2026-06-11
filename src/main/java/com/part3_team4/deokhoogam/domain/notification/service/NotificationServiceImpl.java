package com.part3_team4.deokhoogam.domain.notification.service;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationAccessDeniedException;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationNotFoundException;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationInvalidInputException;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

  /**
   * 알림 Entity를 저장하거나 조회하기 위해 사용하는 Repository입니다.
   */
  private final NotificationRepository notificationRepository;

  /**
   * 로그에서 알림이 생성된 원인을 구분하기 위한 내부 타입입니다.
   *
   * DB에 저장되는 도메인 필드가 아니라 로그 식별 목적으로만 사용합니다.
   * 문자열을 직접 반복해서 사용하지 않도록 Service 내부 enum으로 관리합니다.
   */
  private enum NotificationLogType {
    REACTION,
    LIKE,
    COMMENT,
    POPULAR_REVIEW
  }

  /**
   * 알림을 생성합니다.
   *
   * 이 메서드는 알림을 받을 사용자, 연결된 리뷰, 리뷰 내용, 발신자 이름을 받아
   * Notification Entity를 생성한 뒤 저장합니다.
   *
   * 새 알림은 아직 확인하지 않은 상태이므로 confirmed=false로 저장합니다.
   */
  @Override
  public void createNotification(
      UUID userId,
      UUID reviewId,
      String reviewContent,
      String sender
  ) {
    // 기존 호출부와의 호환성을 유지하기 위한 공통 알림 생성 메서드입니다.
    saveNotification(
        userId,
        reviewId,
        reviewContent,
        sender + "님이 내 리뷰에 반응했습니다.",
        NotificationLogType.REACTION
    );
  }

  /**
   * 리뷰 작성자와 반응을 남긴 사용자가 같은지 확인합니다.
   *
   * @param receiverId 알림을 받을 리뷰 작성자 ID
   * @param actorId 좋아요 또는 댓글을 남긴 사용자 ID
   * @return 자기 행동이면 true, 다른 사용자의 행동이면 false
   */
  private boolean isSelfAction(UUID receiverId, UUID actorId) {
    return receiverId.equals(actorId);
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
    // 자신의 리뷰에 직접 반응한 경우에는 자기 알림을 만들지 않습니다.
    if (isSelfAction(receiverId, actorId)) {
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

    // 알림 상태 변경이 정상적으로 완료된 마지막 시점에 한 번만 기록합니다.
    log.info(
        "알림 확인 상태 변경 완료: notificationId={}, userId={}, confirmed={}",
        notificationId,
        requesterId,
        request.confirmed()
    );

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

    // 몇 개의 알림이 실제로 변경됐는지 운영에서 확인할 수 있도록 기록합니다.
    // 개별 알림마다 로그를 남기지 않고 전체 요청을 하나의 사건으로 기록합니다.
    log.info(
        "전체 알림 읽음 처리 완료: userId={}, updatedCount={}",
        requesterId,
        notifications.size()
    );
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
    // 자신의 리뷰에 좋아요를 누른 경우에는 알림을 만들지 않습니다.
    if (isSelfAction(receiverId, actorId)) {
      return;
    }

    // 좋아요 알림에 맞는 메시지와 함께 Notification 엔티티를 저장합니다.
    saveNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender + "님이 내 리뷰에 좋아요를 눌렀습니다.",
        NotificationLogType.LIKE
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
    // 자신의 리뷰에 댓글을 작성한 경우에는 알림을 만들지 않습니다.
    if (isSelfAction(receiverId, actorId)) {
      return;
    }

    // 댓글 알림에 맞는 메시지와 함께 Notification 엔티티를 저장합니다.
    saveNotification(
        receiverId,
        reviewId,
        reviewContent,
        sender + "님이 내 리뷰에 댓글을 남겼습니다.",
        NotificationLogType.COMMENT
    );
  }

  /**
   * 인기 리뷰 선정 알림을 생성합니다.
   *
   * 요구사항:
   * - 내가 작성한 리뷰의 인기 순위가 각 기간별 10위 내에 선정되면 알림이 생성됩니다.
   *
   * 인기 리뷰 선정은 사용자의 직접 행동이 아니라 배치 결과이므로,
   * 좋아요/댓글 알림과 달리 actorId를 받지 않습니다.
   */
  @Override
  public void createPopularReviewNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String period,
      int rank
  ) {
    // period는 인기 리뷰 기간을 나타내는 필수 값입니다.
    // null이면 switch 문에서 NullPointerException이 발생할 수 있으므로
    // 메시지 생성 전에 명시적으로 검증합니다.
    Objects.requireNonNull(period, "period must not be null");

    // 요구사항은 "각 기간별 10위 내" 선정 시 알림 생성입니다.
    // 따라서 1~10위가 아닌 순위는 알림 생성 대상이 아닙니다.
    if (!isTopTenRank(rank)) {
      return;
    }

    saveNotification(
        receiverId,
        reviewId,
        reviewContent,
        buildPopularReviewMessage(period, rank),
        NotificationLogType.POPULAR_REVIEW
    );
  }

  /**
   * 인기 리뷰 기간 코드를 사용자에게 보여줄 한글 표현으로 변환합니다.
   *
   * 현재 PopularReview 엔티티는 period를 enum이 아니라 String으로 관리합니다.
   * 따라서 알림 도메인도 같은 String 값을 받아 메시지 표시용 문구로 변환합니다.
   *
   * 지원 값:
   * - DAILY -> 일간
   * - WEEKLY -> 주간
   * - MONTHLY -> 월간
   * - ALL_TIME -> 역대
   *
   * 알 수 없는 값은 그대로 사용합니다.
   * 이렇게 하면 대시보드/배치 쪽에서 새로운 period 값을 추가하더라도
   * 알림 생성 자체가 깨지지 않습니다.
   */
  private String toPeriodLabel(String period) {
    return switch (period) {
      case "DAILY" -> "일간";
      case "WEEKLY" -> "주간";
      case "MONTHLY" -> "월간";
      case "ALL_TIME" -> "역대";
      default -> period;
    };
  }

  /**
   * 인기 리뷰 알림 생성 대상 순위인지 확인합니다.
   *
   * 요구사항에서는 각 기간별 10위 이내에 선정된 리뷰에 대해서만
   * 알림을 생성한다고 되어 있습니다.
   *
   * @param rank 인기 리뷰 순위
   * @return 1위부터 10위까지면 true, 그 외 순위면 false
   */
  private boolean isTopTenRank(int rank) {
    return rank >= 1 && rank <= 10;
  }

  /**
   * 인기 리뷰 선정 알림 메시지를 생성합니다.
   *
   * 메시지 형식을 한 곳에서 관리하면,
   * 나중에 문구를 변경할 때 알림 생성 로직을 건드리지 않아도 됩니다.
   */
  private String buildPopularReviewMessage(String period, int rank) {
    return "내 리뷰가 " + toPeriodLabel(period) + " 인기 리뷰 " + rank + "위에 선정되었습니다.";
  }

  /**
   * 알림 유형별 메서드에서 공통으로 사용하는 저장 메서드입니다.
   *
   * Repository 저장이 예외 없이 완료된 이후에만 INFO 로그를 남깁니다.
   * 리뷰 내용과 메시지 전문은 로그 크기 및 개인정보 노출을 고려하여 기록하지 않습니다.
   *
   * @param receiverId 알림 수신자 ID
   * @param reviewId 알림과 연결된 리뷰 ID
   * @param reviewContent 알림 생성 당시 리뷰 내용
   * @param message 사용자에게 표시할 알림 메시지
   * @param notificationType 알림이 생성된 원인
   */
  private void saveNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String message,
      NotificationLogType notificationType
  ) {
    Notification notification = Notification.builder()
        .userId(receiverId)
        .reviewId(reviewId)
        .reviewContent(reviewContent)
        .message(message)
        // 새로 생성된 알림은 아직 사용자가 확인하지 않았으므로 false입니다.
        .confirmed(false)
        .build();

    // save()가 예외 없이 반환된 시점에만 성공 로그를 기록합니다.
    notificationRepository.save(notification);

    log.info(
        "알림 생성 완료: notificationId={}, receiverId={}, reviewId={}, type={}",
        notification.getId(),
        receiverId,
        reviewId,
        notificationType
    );
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
    // 알림은 본인만 조회할 수 있으므로 요청자와 조회 대상 사용자를 먼저 비교합니다.
    // 권한 검증을 가장 먼저 수행하면 권한이 없는 요청에 대해
    // PageRequest 생성이나 Repository 조회가 실행되는 것을 방지할 수 있습니다.
    validateReadPermission(requesterId, userId);

    // cursor와 after는 하나의 커서 위치를 구성하는 값입니다.
    // 하나만 전달되면 잘못된 요청으로 처리합니다.
    validateCursorPair(cursor, after);

    // 다음 페이지 존재 여부를 확인하기 위해 요청한 limit보다 1개 더 조회합니다.
    // 예를 들어 limit이 20이면 최대 21개를 조회합니다.
    PageRequest pageRequest = PageRequest.of(0, limit + 1);

    // 커서 존재 여부에 따라 첫 페이지 또는 다음 페이지를 조회합니다.
    List<Notification> notifications = cursor == null
        ? findFirstPage(userId, direction, pageRequest)
        : findNextPage(userId, direction, cursor, after, pageRequest);

    // 조회 결과를 Swagger 응답 형식인 PageResponse로 변환합니다.
    return createPageResponse(userId, notifications, limit);
  }

  /**
   * 요청자가 조회 대상 사용자의 알림을 조회할 권한이 있는지 검증합니다.
   *
   * 현재 알림 목록 API는 본인의 알림만 조회할 수 있습니다.
   * 따라서 요청 헤더의 사용자 ID와 query parameter의 userId가 같아야 합니다.
   */
  private void validateReadPermission(UUID requesterId, UUID userId) {
    if (!requesterId.equals(userId)) {
      throw NotificationAccessDeniedException.withUserId(userId);
    }
  }

  /**
   * 커서 페이지네이션 파라미터 조합을 검증합니다.
   *
   * 정상적인 조합:
   * - cursor == null, after == null: 첫 페이지 조회
   * - cursor != null, after != null: 다음 페이지 조회
   *
   * 잘못된 조합:
   * - cursor만 존재
   * - after만 존재
   */
  private void validateCursorPair(UUID cursor, Instant after) {
    // 두 null 여부가 서로 다르면 하나만 전달된 상태입니다.
    // XOR와 같은 의미지만 가독성을 위해 명시적인 비교식을 사용합니다.
    boolean onlyOneProvided = (cursor == null) != (after == null);

    if (onlyOneProvided) {
      throw NotificationInvalidInputException.withCursorPair(cursor, after);
    }
  }

  /**
   * 커서가 없는 첫 페이지를 조회합니다.
   *
   * ASC는 오래된 알림부터, DESC는 최신 알림부터 조회합니다.
   */
  private List<Notification> findFirstPage(
      UUID userId,
      Sort.Direction direction,
      PageRequest pageRequest
  ) {
    if (direction == Sort.Direction.ASC) {
      return notificationRepository
          .findByUserIdOrderByCreatedAtAscIdAsc(userId, pageRequest);
    }

    return notificationRepository
        .findByUserIdOrderByCreatedAtDescIdDesc(userId, pageRequest);
  }

  /**
   * cursor와 after를 기준으로 다음 페이지를 조회합니다.
   *
   * createdAt만으로 커서를 구성하면 동일한 시각에 생성된 알림 사이에서
   * 중복 또는 누락이 발생할 수 있습니다. 따라서 ID를 보조 정렬 조건으로 사용합니다.
   */
  private List<Notification> findNextPage(
      UUID userId,
      Sort.Direction direction,
      UUID cursor,
      Instant after,
      PageRequest pageRequest
  ) {
    if (direction == Sort.Direction.ASC) {
      return notificationRepository.findNextPageAsc(
          userId,
          after,
          cursor,
          pageRequest
      );
    }

    return notificationRepository.findNextPageDesc(
        userId,
        after,
        cursor,
        pageRequest
    );
  }

  /**
   * 조회된 알림 목록을 PageResponse로 변환합니다.
   *
   * Repository에서는 hasNext 판단을 위해 limit + 1개를 조회하지만,
   * 실제 응답에는 사용자가 요청한 limit 개수까지만 포함합니다.
   */
  private PageResponse<NotificationDto> createPageResponse(
      UUID userId,
      List<Notification> notifications,
      int limit
  ) {
    // limit보다 많이 조회됐다면 다음 페이지가 존재합니다.
    boolean hasNext = notifications.size() > limit;

    // 응답에는 최대 limit개의 알림만 포함합니다.
    List<Notification> pageContent = hasNext
        ? notifications.subList(0, limit)
        : notifications;

    List<NotificationDto> content = pageContent.stream()
        .map(NotificationDto::from)
        .toList();

    String nextCursor = null;
    String nextAfter = null;

    // 다음 페이지가 있을 때만 현재 응답의 마지막 알림으로
    // 다음 요청에 사용할 cursor와 after를 생성합니다.
    if (hasNext && !pageContent.isEmpty()) {
      Notification lastNotification =
          pageContent.get(pageContent.size() - 1);

      nextCursor = lastNotification.getId().toString();
      nextAfter = lastNotification.getCreatedAt().toString();
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