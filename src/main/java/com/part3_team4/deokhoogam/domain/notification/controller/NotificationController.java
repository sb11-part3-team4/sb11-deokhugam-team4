package com.part3_team4.deokhoogam.domain.notification.controller;

import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.domain.notification.exception.NotificationInvalidInputException;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

  /**
   * 사용자의 알림 목록을 조회합니다.
   *
   * Swagger 명세:
   * GET /api/notifications
   */
  @Operation(
      summary = "알림 목록 조회",
      description = "사용자의 알림 목록을 최근 시간 순으로 조회합니다."
  )
  @GetMapping
  public ResponseEntity<PageResponse<NotificationDto>> findAll(
      @Parameter(description = "요청자 ID", required = true)
      @RequestHeader("Deokhugam-Request-User-ID") UUID requesterId,

      @Parameter(description = "사용자 ID", required = true)
      @RequestParam UUID userId,

      @Parameter(description = "정렬 방향", example = "DESC")
      @RequestParam(defaultValue = "DESC") String direction,

      @Parameter(description = "커서 페이지네이션 커서")
      @RequestParam(required = false) UUID cursor,

      @Parameter(description = "보조 커서(createdAt)")
      @RequestParam(required = false) Instant after,

      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(defaultValue = "20")
      int limit
  ) {
    // limit은 외부 요청에서 직접 들어오는 값이므로 서비스로 넘기기 전에 검증합니다.
    // limit이 0이면 빈 목록인데 다음 페이지가 있다고 판단되는 이상한 응답이 나올 수 있습니다.
    // limit이 음수이면 PageRequest 생성 과정에서 예외가 발생할 수 있습니다.
    // limit이 너무 크면 한 번에 많은 데이터를 조회하므로 서버와 DB에 부담이 됩니다.
    if (limit < 1 || limit > 100) {
      throw NotificationInvalidInputException.withLimit(limit);
    }


    /**
     * direction query parameter는 Swagger에서 ASC/DESC로 안내하지만,
     * 실제 클라이언트가 desc, asc처럼 소문자로 보낼 수도 있습니다.
     *
     * Spring MVC가 Sort.Direction enum으로 직접 변환하게 두면
     * 환경에 따라 대소문자 차이로 400 Bad Request가 발생할 수 있습니다.
     *
     * 따라서 Controller에서 문자열로 받은 뒤 Sort.Direction.fromString(...)으로 변환합니다.
     * fromString은 "DESC", "desc" 모두 처리할 수 있습니다.
     */

    // 클라이언트가 소문자 asc/desc를 전달해도 처리할 수 있도록
    // 별도의 변환 메서드를 통해 Sort.Direction으로 변환합니다.
    Sort.Direction sortDirection = parseDirection(direction);

    PageResponse<NotificationDto> response = notificationService.findAll(
        requesterId,
        userId,
        sortDirection,
        cursor,
        after,
        limit
    );

    return ResponseEntity.ok(response);
  }

  /**
   * HTTP query parameter로 전달된 정렬 방향을 변환합니다.
   *
   * Sort.Direction.fromString()은 "ASC", "DESC"뿐만 아니라
   * "asc", "desc"와 같은 소문자 입력도 처리할 수 있습니다.
   *
   * 지원하지 않는 값이 전달되면 Spring의 내부 예외를 그대로 노출하지 않고,
   * 알림 도메인의 입력값 예외로 변환하여 400 Bad Request를 반환합니다.
   *
   * @param direction 사용자가 전달한 정렬 방향 문자열
   * @return 변환된 Spring Data 정렬 방향
   */
  private Sort.Direction parseDirection(String direction) {
    try {
      return Sort.Direction.fromString(direction);
    } catch (IllegalArgumentException exception) {
      throw NotificationInvalidInputException.withDirection(direction);
    }
  }
}