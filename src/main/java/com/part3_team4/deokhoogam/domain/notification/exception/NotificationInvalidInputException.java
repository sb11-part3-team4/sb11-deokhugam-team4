package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

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
}