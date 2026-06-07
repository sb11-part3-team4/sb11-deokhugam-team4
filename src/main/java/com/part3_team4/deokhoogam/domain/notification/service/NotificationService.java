package com.part3_team4.deokhoogam.domain.notification.service;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Sort;

/**
 * 알림 도메인의 비즈니스 로직을 정의하는 Service 인터페이스입니다.
 *
 * Controller나 다른 도메인은 구현체(NotificationServiceImpl)가 아니라
 * 이 인터페이스에 의존하게 만드는 것이 일반적입니다.
 */
public interface NotificationService {

  /**
   * 알림을 생성합니다.
   *
   * 이 메서드는 기존 코드와의 호환을 위해 유지합니다.
   *
   * @param userId 알림을 받을 사용자 ID
   * @param reviewId 알림과 연결된 리뷰 ID
   * @param reviewContent 리뷰 내용
   * @param sender 알림을 발생시킨 사용자 이름
   */
  void createNotification(UUID userId, UUID reviewId, String reviewContent, String sender);

  /**
   * 알림을 생성합니다.
   *
   * actorId를 추가로 받아서 자기 행동 알림을 방지할 수 있습니다.
   *
   * 예:
   * - 내가 내 리뷰에 댓글을 단 경우 알림 생성 X
   * - 다른 사용자가 내 리뷰에 댓글을 단 경우 알림 생성 O
   *
   * @param receiverId 알림을 받을 사용자 ID
   * @param reviewId 알림과 연결된 리뷰 ID
   * @param reviewContent 리뷰 내용
   * @param sender 알림을 발생시킨 사용자 이름
   * @param actorId 알림을 발생시킨 사용자 ID
   */
  void createNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  );

  /**
   * 특정 알림의 확인 여부를 수정합니다.
   *
   * 요구사항:
   * - 알림을 확인하면 확인 여부를 수정합니다.
   * - 본인의 알림만 수정할 수 있어야 합니다.
   *
   * @param notificationId 수정할 알림 ID
   * @param requesterId 요청자 ID
   * @param request 수정할 confirmed 값
   * @return 수정된 알림 응답 DTO
   */
  NotificationDto updateConfirmed(
      UUID notificationId,
      UUID requesterId,
      NotificationUpdateRequest request
  );

  /**
   * 요청자의 모든 미확인 알림을 읽음 처리합니다.
   *
   * 요구사항:
   * - 모든 알림을 한번에 확인할 수 있습니다.
   *
   * @param requesterId 요청자 ID
   */
  void readAll(UUID requesterId);

  /**
   * 리뷰에 좋아요가 발생했을 때 알림을 생성합니다.
   *
   * 다른 도메인의 좋아요 처리 로직이 호출할 알림 도메인의 진입점입니다.
   * 리뷰 작성자와 좋아요를 누른 사용자가 같으면 알림을 생성하지 않습니다.
   *
   * @param receiverId 알림을 받을 리뷰 작성자 ID
   * @param reviewId 좋아요가 발생한 리뷰 ID
   * @param reviewContent 알림에 표시할 리뷰 내용
   * @param sender 좋아요를 누른 사용자 닉네임
   * @param actorId 좋아요를 누른 사용자 ID
   */
  void createLikeNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  );

  /**
   * 리뷰에 댓글이 작성되었을 때 알림을 생성합니다.
   *
   * 다른 도메인의 댓글 등록 로직이 호출할 알림 도메인의 진입점입니다.
   * 리뷰 작성자와 댓글 작성자가 같으면 알림을 생성하지 않습니다.
   *
   * @param receiverId 알림을 받을 리뷰 작성자 ID
   * @param reviewId 댓글이 작성된 리뷰 ID
   * @param reviewContent 알림에 표시할 리뷰 내용
   * @param sender 댓글 작성자 닉네임
   * @param actorId 댓글 작성자 ID
   */
  void createCommentNotification(
      UUID receiverId,
      UUID reviewId,
      String reviewContent,
      String sender,
      UUID actorId
  );

  /**
   * 사용자의 알림 목록을 커서 페이지네이션으로 조회합니다.
   *
   * @param userId 알림을 조회할 사용자 ID
   * @param direction 정렬 방향
   * @param cursor 이전 페이지 마지막 알림 ID
   * @param after 이전 페이지 마지막 알림 생성 시각
   * @param limit 페이지 크기
   * @return 알림 목록 페이지 응답
   */
  PageResponse<NotificationDto> findAll(
      UUID requesterId,
      UUID userId,
      Sort.Direction direction,
      UUID cursor,
      Instant after,
      int limit
  );
}