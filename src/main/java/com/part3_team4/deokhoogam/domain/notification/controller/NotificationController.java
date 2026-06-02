package com.part3_team4.deokhoogam.domain.notification.controller;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * 알림 관련 HTTP API를 처리하는 Controller입니다.
 *
 * Controller의 책임:
 * - HTTP 요청을 받는다.
 * - PathVariable, Header, RequestBody 값을 Java 타입으로 변환한다.
 * - Service 계층을 호출한다.
 * - Service 결과를 HTTP 응답으로 변환한다.
 *
 * 비즈니스 로직은 Controller에 두지 않고 NotificationService에 위임합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  /**
   * 알림 비즈니스 로직을 처리하는 Service입니다.
   *
   * Controller는 직접 Repository를 호출하지 않습니다.
   * 요청을 해석한 뒤 Service에 필요한 값만 넘깁니다.
   */
  private final NotificationService notificationService;

  /**
   * 특정 알림의 읽음 상태를 수정합니다.
   *
   * Swagger 명세:
   * PATCH /api/notifications/{notificationId}
   *
   * 필요한 값:
   * - notificationId: path variable
   * - Deokhugam-Request-User-ID: 요청자 ID header
   * - confirmed: request body
   *
   * 성공 응답:
   * - 200 OK
   * - 수정된 NotificationDto 반환
   */
  @Operation(
      summary = "알림 읽음 상태 업데이트",
      description = "특정 알림의 상태를 업데이트 합니다."
  )
  @PatchMapping("/{notificationId}")
  public ResponseEntity<NotificationDto> updateConfirmed(
      @Parameter(
          description = "알림 ID",
          required = true,
          example = "123e4567-e89b-12d3-a456-426614174000"
      )
      @PathVariable UUID notificationId,

      @Parameter(
          description = "요청자 ID",
          required = true,
          example = "123e4567-e89b-12d3-a456-426614174000"
      )
      @RequestHeader("Deokhugam-Request-User-ID") UUID requesterId,

      @Valid @RequestBody NotificationUpdateRequest request
  ) {
    NotificationDto response = notificationService.updateConfirmed(
        notificationId,
        requesterId,
        request
    );

    return ResponseEntity.ok(response);
  }

  /**
   * 요청자의 모든 알림을 읽음 처리합니다.
   *
   * Swagger 명세:
   * PATCH /api/notifications/read-all
   *
   * 필요한 값:
   * - Deokhugam-Request-User-ID: 요청자 ID header
   *
   * 성공 응답:
   * - 204 No Content
   * - 응답 body 없음
   */
  @Operation(
      summary = "모든 알림 읽음 처리",
      description = "사용자의 모든 알림을 읽음 상태로 처리합니다."
  )
  @PatchMapping("/read-all")
  public ResponseEntity<Void> readAll(
      @Parameter(
          description = "요청자 ID",
          required = true,
          example = "123e4567-e89b-12d3-a456-426614174000"
      )
      @RequestHeader("Deokhugam-Request-User-ID") UUID requesterId
  ) {
    notificationService.readAll(requesterId);

    return ResponseEntity.noContent().build();
  }
}