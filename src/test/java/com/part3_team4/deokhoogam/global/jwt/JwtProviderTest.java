package com.part3_team4.deokhoogam.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtProviderTest {

  private final String testSecretKey = "dmVyeS12ZXJ5LXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZS1kZW9raG9vZ2FtLXRlYW00";
  private final long testExpiration = 3600000L;

  private final JwtProvider jwtProvider = new JwtProvider(testSecretKey, testExpiration);

  @Test
  @DisplayName("이메일을 전달하면 JWT 액세스 토큰 발급")
  void createAccessToken() {
    UUID userId = UUID.randomUUID();

    String token = jwtProvider.createAccessToken(userId);

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @DisplayName("유효한 토큰을 검증하면 true 반환")
  void validateToken_Valid() {
    UUID userId = UUID.randomUUID();
    String token = jwtProvider.createAccessToken(userId);

    boolean isValid = jwtProvider.validateToken(token);

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("유효하지 않은 토큰을 검증하면 false 반환")
  void validateToken_Invalid() {
    String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid.payload.signature";

    boolean isValid = jwtProvider.validateToken(invalidToken);

    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("유효한 토큰에서 Authentication 객체를 정상적으로 추출")
  void getAuthentication() {
    UUID userId = UUID.randomUUID();
    String token = jwtProvider.createAccessToken(userId);

    Authentication authentication = jwtProvider.getAuthentication(token);

    assertThat(authentication).isNotNull();

    assertThat(authentication.getPrincipal()).isEqualTo(userId);
    assertThat(authentication.getName()).isEqualTo(userId.toString());
  }
}