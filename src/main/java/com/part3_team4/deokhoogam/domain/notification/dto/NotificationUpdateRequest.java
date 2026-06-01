package com.part3_team4.deokhoogam.domain.notification.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 알림 읽음 상태 수정 요청 DTO입니다.
 *
 * Swagger의 PATCH /api/notifications/{notificationId} 요청 body는
 * 아래와 같은 형태입니다.
 *
 * {
 *   "confirmed": true
 * }
 *
 * confirmed는 반드시 전달되어야 하므로 @NotNull을 붙입니다.
 * boolean이 아니라 Boolean을 사용하는 이유는
 * 요청에서 필드가 누락된 경우 null로 검증할 수 있게 하기 위해서입니다.
 */
public record NotificationUpdateRequest(
    @NotNull Boolean confirmed
) {
}