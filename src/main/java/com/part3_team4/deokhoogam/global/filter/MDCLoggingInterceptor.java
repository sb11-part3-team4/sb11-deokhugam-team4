package com.part3_team4.deokhoogam.global.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class MDCLoggingInterceptor implements HandlerInterceptor {

  public static final String REQUEST_ID = "requestId";
  public static final String REQUEST_IP = "requestIp";
  public static final String REQUEST_METHOD = "requestMethod";
  public static final String REQUEST_URI = "requestUri";
  public static final String START_TIME = "startTime";   // 추가


  //요청 아이디 추가
  public static final String REQUEST_ID_HEADER = "Deokhugam-Request-ID";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      @NonNull Object handler) {

    // 알림 폴링 요청은 스킵
    if ("GET".equals(request.getMethod())
        && "/api/notifications".equals(request.getRequestURI())) {
      return true;
    }

    // 요청 ID 생성 (UUID)
    String requestId = UUID.randomUUID().toString().replaceAll("-", "");
    String clientIp = extractClientIp(request);

    // MDC에 컨텍스트 정보 추가
    MDC.put(REQUEST_ID, requestId);
    MDC.put(REQUEST_IP, clientIp);
    MDC.put(REQUEST_METHOD, request.getMethod());
    MDC.put(REQUEST_URI, request.getRequestURI());
    MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));


    // 응답 헤더에 요청 ID 추가
    response.setHeader(REQUEST_ID_HEADER, requestId);

    log.info("Request started: {} {}", request.getMethod(), request.getRequestURI());
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {

    long duration = -1;
    String startTime = MDC.get(START_TIME);
    if (startTime != null) { // startTime null 체크
      try {
        duration = System.currentTimeMillis() - Long.parseLong(startTime);
      } catch (NumberFormatException e) {
        log.warn("startTime 파싱 실패: {}", startTime);
      }
    }
    // 몇초 걸렸는지 출력
    log.info("Request completed: {} {} - status={} ({}ms)",
        request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
    // 요청 처리 후 MDC 데이터 정리
    MDC.clear();
  }

  private static final Pattern IP_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]+$");

  private String extractClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      String candidate = ip.split(",")[0].trim();
      if (IP_PATTERN.matcher(candidate).matches()) { //패턴 검사(유효한 IP 형식)
        return candidate;
      }
    }
    return request.getRemoteAddr();
  }
}


