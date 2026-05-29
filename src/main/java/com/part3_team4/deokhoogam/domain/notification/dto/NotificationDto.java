package com.part3_team4.deokhoogam.domain.notification.dto;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;

/**
 * 알림 응답 DTO입니다.
 *
 * Swagger 응답 스펙에 맞춰 필드를 구성합니다.
 * Controller는 Entity를 그대로 반환하지 않고 이 DTO를 반환해야 합니다.
 *
 * Entity를 직접 반환하지 않는 이유:
 * - DB 구조가 API 응답에 그대로 노출되는 것을 막기 위해
 * - 응답 필드명을 Swagger와 안정적으로 맞추기 위해
 * - 나중에 Entity 구조가 바뀌어도 API 응답을 유지하기 위해
 */
public record NotificationDto(
    UUID id,
    UUID userId,
    UUID reviewId,
    String reviewContent,
    String message,
    boolean confirmed,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Notification Entity를 NotificationDto로 변환하는 정적 팩토리 메서드입니다.
   *
   * Service 계층에서 알림 조회/수정 후 응답 DTO를 만들 때 사용합니다.
   */
  public static NotificationDto from(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getUserId(),
        notification.getReviewId(),
        notification.getReviewContent(),
        notification.getMessage(),
        notification.isConfirmed(),
        notification.getCreatedAt(),
        notification.getUpdatedAt()
    );
  }
}