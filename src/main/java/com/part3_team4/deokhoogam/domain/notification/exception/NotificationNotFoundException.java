package com.part3_team4.deokhoogam.domain.notification.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {

  private NotificationNotFoundException() {
    super(ErrorCode.NOTIFICATION_NOT_FOUND);
  }

  public static NotificationNotFoundException withId(UUID notificationId) {
    NotificationNotFoundException exception = new NotificationNotFoundException();
    exception.addDetail(ErrorKey.NOTIFICATION_ID, notificationId);
    return exception;
  }
}