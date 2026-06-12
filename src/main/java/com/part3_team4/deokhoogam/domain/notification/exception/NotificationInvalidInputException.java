package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.time.Instant;
import java.util.UUID;

/**
 * 알림 API 요청값이 올바르지 않을 때 사용하는 예외입니다.
 *
 * 예:
 * - limit이 1보다 작거나 100보다 큰 경우
 * - direction 값이 ASC/DESC가 아닌 경우
 *
 * 이 예외는 BusinessException 계열이므로
 * GlobalExceptionHandler의 handleBusinessException에서 처리되고,
 * ErrorCode.INVALID_INPUT_VALUE에 따라 400 Bad Request로 응답됩니다.
 */
public class NotificationInvalidInputException extends NotificationException {

  private NotificationInvalidInputException() {
    super(ErrorCode.INVALID_INPUT_VALUE);
  }

  /**
   * limit 파라미터가 허용 범위를 벗어났을 때 사용합니다.
   *
   * @param limit 사용자가 요청한 페이지 크기
   * @return limit 오류 정보를 details에 담은 예외
   */
  public static NotificationInvalidInputException withLimit(int limit) {
    NotificationInvalidInputException exception = new NotificationInvalidInputException();

    exception.addDetail(ErrorKey.FIELD, "limit");
    exception.addDetail(ErrorKey.VALUE, limit);
    exception.addDetail(ErrorKey.REASON, "limit은 1 이상 100 이하만 허용됩니다.");

    return exception;
  }

  /**
   * direction 파라미터가 ASC/DESC로 해석될 수 없을 때 사용합니다.
   *
   * @param direction 사용자가 요청한 정렬 방향 문자열
   * @return direction 오류 정보를 details에 담은 예외
   */
  public static NotificationInvalidInputException withDirection(String direction) {
    NotificationInvalidInputException exception = new NotificationInvalidInputException();

    exception.addDetail(ErrorKey.FIELD, "direction");
    exception.addDetail(ErrorKey.VALUE, direction);
    exception.addDetail(ErrorKey.REASON, "direction은 ASC 또는 DESC만 허용됩니다.");

    return exception;
  }

  /**
   * 커서 페이지네이션 파라미터가 올바른 조합으로 전달되지 않았을 때 사용합니다.
   *
   * 알림 목록의 다음 페이지를 정확히 조회하려면 다음 두 값이 함께 필요합니다.
   * - cursor: 이전 페이지 마지막 알림의 ID
   * - after: 이전 페이지 마지막 알림의 생성 시각
   *
   * 둘 중 하나만 전달되면 조회 기준을 정확히 결정할 수 없으므로
   * 첫 페이지로 조용히 처리하지 않고 400 Bad Request를 반환합니다.
   *
   * @param cursor 이전 페이지 마지막 알림 ID
   * @param after 이전 페이지 마지막 알림 생성 시각
   * @return 잘못된 커서 조합 정보를 담은 예외
   */
  public static NotificationInvalidInputException withCursorPair(
      UUID cursor,
      Instant after
  ) {
    NotificationInvalidInputException exception =
        new NotificationInvalidInputException();

    exception.addDetail(ErrorKey.FIELD, "cursor, after");
    exception.addDetail(
        ErrorKey.VALUE,
        "cursor=" + cursor + ", after=" + after
    );
    exception.addDetail(
        ErrorKey.REASON,
        "cursor와 after는 함께 전달하거나 모두 생략해야 합니다."
    );

    return exception;
  }
}