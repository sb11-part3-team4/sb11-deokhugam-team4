package com.part3_team4.deokhoogam.domain.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.notification.controller.NotificationController;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationDto;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import com.part3_team4.deokhoogam.domain.notification.dto.NotificationUpdateRequest;
import com.part3_team4.deokhoogam.global.jwt.JwtFilter;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Sort;

/**
 * NotificationController의 HTTP API 계약을 검증하는 테스트입니다.
 *
 * 이 테스트는 Service 로직 자체를 검증하지 않습니다.
 * Service 로직은 NotificationServiceTest에서 이미 검증했습니다.
 *
 * 여기서는 Swagger 명세에 맞게 다음을 검증합니다.
 * - URL
 * - HTTP Method
 * - Header
 * - Request Body
 * - Response Status
 * - Response JSON
 * - Controller가 Service를 올바르게 호출하는지
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

  /**
   * MockMvc는 실제 서버를 띄우지 않고 Controller API를 호출할 수 있게 해주는 테스트 도구입니다.
   *
   * 예:
   * mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId))
   */
  @Autowired
  private MockMvc mockMvc;

  /**
   * Java 객체를 JSON 문자열로 변환할 때 사용합니다.
   *
   * PATCH 요청 body로 { "confirmed": true }를 보내기 위해 사용합니다.
   */
  @Autowired
  private ObjectMapper objectMapper;

  /**
   * Controller가 의존하는 Service를 mock 객체로 대체합니다.
   *
   * @WebMvcTest는 Controller 계층만 테스트하므로,
   * 실제 NotificationService 구현체는 Spring Bean으로 등록되지 않습니다.
   *
   * 따라서 MockitoBean으로 가짜 Service Bean을 등록해줍니다.
   */
  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private JwtFilter jwtFilter;

  @Test
  @DisplayName("PATCH /api/notifications/{notificationId} 요청으로 알림 읽음 상태를 수정한다")
  void updateConfirmed() throws Exception {
    // given
    // 수정할 알림 ID입니다.
    UUID notificationId = UUID.randomUUID();

    // 요청자 ID입니다.
    // Swagger 명세에 따르면 Deokhugam-Request-User-ID 헤더로 전달됩니다.
    UUID requesterId = UUID.randomUUID();

    // 알림과 연결된 리뷰 ID입니다.
    UUID reviewId = UUID.randomUUID();

    // Service가 반환할 응답 DTO를 미리 준비합니다.
    // Controller 테스트에서는 Service 내부 로직을 검증하지 않으므로,
    // Service가 이 값을 반환한다고 가정하고 HTTP 응답이 이 값대로 내려오는지 검증합니다.
    NotificationDto response = new NotificationDto(
        notificationId,
        requesterId,
        reviewId,
        "이 책은 문장들이 오래 남는 리뷰입니다.",
        "우디님이 내 리뷰에 반응했습니다.",
        true,
        Instant.parse("2026-05-29T00:00:00Z"),
        Instant.parse("2026-05-29T00:00:00Z")
    );

    // notificationService.updateConfirmed(...)가 호출되면
    // 위에서 만든 response를 반환하도록 설정합니다.
    given(notificationService.updateConfirmed(
        eq(notificationId),
        eq(requesterId),
        any(NotificationUpdateRequest.class)
    )).willReturn(response);

    // PATCH 요청 body입니다.
    // 실제 API 요청 DTO인 NotificationUpdateRequest를 JSON으로 변환합니다.
    // 생성되는 JSON은 Swagger 명세의 예시와 동일하게 { "confirmed": true } 형태입니다.
    String requestBody = objectMapper.writeValueAsString(
        new NotificationUpdateRequest(true)
    );

    // when & then
    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Deokhugam-Request-User-ID", requesterId)
            .contentType(APPLICATION_JSON)
            .content(requestBody))
        // Swagger 명세상 성공 응답은 200 OK입니다.
        .andExpect(status().isOk())

        // 응답 Content-Type이 JSON인지 확인합니다.
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))

        // 응답 JSON 필드가 Swagger NotificationDto 스펙과 맞는지 검증합니다.
        .andExpect(jsonPath("$.id").value(notificationId.toString()))
        .andExpect(jsonPath("$.userId").value(requesterId.toString()))
        .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
        .andExpect(jsonPath("$.reviewContent").value("이 책은 문장들이 오래 남는 리뷰입니다."))
        .andExpect(jsonPath("$.message").value("우디님이 내 리뷰에 반응했습니다."))
        .andExpect(jsonPath("$.confirmed").value(true))
        .andExpect(jsonPath("$.createdAt").value("2026-05-29T00:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-05-29T00:00:00Z"));

    // Controller가 Service를 정확한 인자로 호출했는지 검증합니다.
    then(notificationService)
        .should()
        .updateConfirmed(
            eq(notificationId),
            eq(requesterId),
            any(NotificationUpdateRequest.class)
        );
  }

  @Test
  @DisplayName("PATCH /api/notifications/read-all 요청으로 모든 알림을 읽음 처리한다")
  void readAll() throws Exception {
    // given
    // 요청자 ID입니다.
    // Swagger 명세에 따르면 Deokhugam-Request-User-ID 헤더로 전달됩니다.
    UUID requesterId = UUID.randomUUID();

    // when & then
    mockMvc.perform(patch("/api/notifications/read-all")
            .header("Deokhugam-Request-User-ID", requesterId))
        // Swagger 명세상 전체 읽음 처리 성공 응답은 204 No Content입니다.
        .andExpect(status().isNoContent())
        // 204 응답은 body가 없어야 합니다.
        .andExpect(content().string(""));

    // Controller가 Service의 readAll 메서드를 요청자 ID로 호출했는지 검증합니다.
    then(notificationService)
        .should()
        .readAll(requesterId);
  }

  @Test
  @DisplayName("GET /api/notifications 요청에서 direction 소문자 desc를 DESC로 처리한다")
  void findAllWithLowercaseDirection() throws Exception {
    // given
    // 요청자 ID입니다.
    // 알림 목록 조회는 본인의 알림만 조회할 수 있어야 하므로,
    // 요청자 ID와 조회 대상 userId를 같은 값으로 둡니다.
    UUID requesterId = UUID.randomUUID();

    // 조회 대상 사용자 ID입니다.
    // 이 테스트에서는 requesterId와 같으므로 권한 검증을 통과하는 상황입니다.
    UUID userId = requesterId;

    // Service가 반환할 빈 페이지 응답입니다.
    // 이 테스트의 목적은 목록 데이터 자체가 아니라
    // direction=desc가 Sort.Direction.DESC로 변환되는지 검증하는 것입니다.
    PageResponse<NotificationDto> response = new PageResponse<>(
        List.of(),
        null,
        null,
        0,
        0L,
        false
    );

    // direction=desc 요청이 들어와도 Controller에서 Sort.Direction.DESC로 변환한 뒤
    // Service를 호출해야 합니다.
    given(notificationService.findAll(
        eq(requesterId),
        eq(userId),
        eq(Sort.Direction.DESC),
        isNull(),
        isNull(),
        eq(20)
    )).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/notifications")
            // 알림 목록 조회 API도 요청자 헤더를 필요로 합니다.
            .header("Deokhugam-Request-User-ID", requesterId)
            // Swagger 명세상 userId는 query parameter입니다.
            .param("userId", userId.toString())
            // CodeRabbit 리뷰 대응 핵심입니다.
            // 소문자 desc를 보내도 정상적으로 DESC로 해석되어야 합니다.
            .param("direction", "desc")
            .param("limit", "20"))
        // 소문자 direction 때문에 400이 발생하면 안 됩니다.
        .andExpect(status().isOk())
        // 빈 페이지 응답이라도 PageResponse JSON 구조는 내려와야 합니다.
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.size").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.hasNext").value(false));

    // Controller가 direction 문자열을 Sort.Direction.DESC로 변환해서
    // Service에 전달했는지 검증합니다.
    then(notificationService)
        .should()
        .findAll(
            eq(requesterId),
            eq(userId),
            eq(Sort.Direction.DESC),
            isNull(),
            isNull(),
            eq(20)
        );
  }

  @Test
  @DisplayName("GET /api/notifications 요청에서 limit이 1보다 작으면 400 Bad Request를 반환한다")
  void findAllWithInvalidLimit() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(get("/api/notifications")
            .header("Deokhugam-Request-User-ID", userId)
            .param("userId", userId.toString())
            .param("limit", "0")) // limit은 컨트롤러에서 1 이상 100 이하로 직접 검증하므로 0은 허용되지 않습니다.
        .andExpect(status().isBadRequest());

    // 컨트롤러 파라미터 검증에서 요청이 막혀야 하므로,
    // 서비스 목록 조회 로직은 호출되지 않아야 합니다.
    then(notificationService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("GET /api/notifications 요청에서 direction 값이 ASC 또는 DESC가 아니면 400 Bad Request를 반환한다")
  void findAllWithInvalidDirection() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(get("/api/notifications")
            .header("Deokhugam-Request-User-ID", userId)
            .param("userId", userId.toString())
            .param("direction", "wrong")
            .param("limit", "20"))
        .andExpect(status().isBadRequest());

    // direction 값이 잘못되면 컨트롤러에서 요청을 막아야 하므로
    // 서비스 목록 조회 로직은 호출되지 않아야 합니다.
    then(notificationService).shouldHaveNoInteractions();
  }
}