package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

/**
 * 알림 접근 권한이 없을 때 발생하는 예외입니다.
 *
 * 예:
 * - A 사용자의 알림을 B 사용자가 읽음 처리하려는 경우
 *
 * Swagger의 PATCH /api/notifications/{notificationId} 명세에서는
 * 이 상황을 403 Forbidden으로 정의하고 있습니다.
 */
public class NotificationAccessDeniedException extends NotificationException {

  private NotificationAccessDeniedException() {
    super(ErrorCode.NOTIFICATION_ACCESS_DENIED);
  }

  /**
   * 권한 문제가 발생한 알림 ID를 예외 상세 정보에 담아 반환합니다.
   *
   * GlobalExceptionHandler가 details를 응답에 포함한다면,
   * 어떤 알림에서 문제가 발생했는지 클라이언트나 로그에서 확인할 수 있습니다.
   */
  public static NotificationAccessDeniedException withId(UUID notificationId) {
    NotificationAccessDeniedException exception = new NotificationAccessDeniedException();
    exception.addDetail(ErrorKey.NOTIFICATION_ID, notificationId);
    return exception;
  }

  public static NotificationAccessDeniedException withUserId(UUID userId) {
    NotificationAccessDeniedException exception = new NotificationAccessDeniedException();
    exception.addDetail(ErrorKey.USER_ID, userId);
    return exception;
  }
}