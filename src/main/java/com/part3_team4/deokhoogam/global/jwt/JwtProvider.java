package com.part3_team4.deokhoogam.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
  private final SecretKey key;
  private final long accessTokenExpTime;

  public JwtProvider(
      @Value("${jwt.secret}") String secretKey,
      @Value("${jwt.expiration_time}") long accessTokenExpTime) {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenExpTime = accessTokenExpTime;
  }

  public String createAccessToken(UUID userId) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + accessTokenExpTime);

    return Jwts.builder()
        .subject(userId.toString())
        .issuedAt(now)
        .expiration(expiration)
        .signWith(key)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public Authentication getAuthentication(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    String userId = claims.getSubject();

    return new UsernamePasswordAuthenticationToken(UUID.fromString(userId), "", Collections.emptyList());
  }
}
