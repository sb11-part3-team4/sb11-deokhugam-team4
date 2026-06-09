package com.part3_team4.deokhoogam.global.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MDCLoggingInterceptorTest {

  private MDCLoggingInterceptor interceptor;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    interceptor = new MDCLoggingInterceptor();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("preHandle 호출 시 MDC에 requestId가 세팅된다")
  void preHandle_setsRequestId() throws Exception {
    interceptor.preHandle(request, response, new Object());

    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_ID)).isNotNull();
  }

  @Test
  @DisplayName("preHandle 호출 시 MDC에 requestIp가 세팅된다")
  void preHandle_setsRequestIp() throws Exception {
    request.setRemoteAddr("127.0.0.1");

    interceptor.preHandle(request, response, new Object());

    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_IP)).isEqualTo("127.0.0.1");
  }

  @Test
  @DisplayName("X-Forwarded-For 헤더가 있으면 해당 IP를 사용한다")
  void preHandle_usesXForwardedForIp() throws Exception {
    request.addHeader("X-Forwarded-For", "121.78.1.1, 10.0.0.1");

    interceptor.preHandle(request, response, new Object());

    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_IP)).isEqualTo("121.78.1.1");
  }

  @Test
  @DisplayName("preHandle 호출 시 MDC에 requestMethod, requestUri가 세팅된다")
  void preHandle_setsRequestMethodAndUri() throws Exception {
    request.setMethod("POST");
    request.setRequestURI("/api/reviews");

    interceptor.preHandle(request, response, new Object());

    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_METHOD)).isEqualTo("POST");
    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_URI)).isEqualTo("/api/reviews");
  }

  @Test
  @DisplayName("preHandle 호출 시 응답 헤더에 requestId가 세팅된다")
  void preHandle_setsResponseHeader() throws Exception {
    interceptor.preHandle(request, response, new Object());

    String requestId = MDC.get(MDCLoggingInterceptor.REQUEST_ID);
    assertThat(response.getHeader(MDCLoggingInterceptor.REQUEST_ID_HEADER)).isEqualTo(requestId);
  }

  @Test
  @DisplayName("afterCompletion 호출 시 MDC가 비워진다")
  void afterCompletion_clearsMDC() throws Exception {
    interceptor.preHandle(request, response, new Object());
    interceptor.afterCompletion(request, response, new Object(), null);

    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_ID)).isNull();
    assertThat(MDC.get(MDCLoggingInterceptor.REQUEST_IP)).isNull();
  }

  @Test
  @DisplayName("preHandle 호출 시 startTime이 MDC에 세팅된다")
  void preHandle_setsStartTime() throws Exception {
    long before = System.currentTimeMillis();
    interceptor.preHandle(request, response, new Object());
    long after = System.currentTimeMillis();

    String startTime = MDC.get(MDCLoggingInterceptor.START_TIME);
    assertThat(startTime).isNotNull();
    assertThat(Long.parseLong(startTime)).isBetween(before, after);
  }
}