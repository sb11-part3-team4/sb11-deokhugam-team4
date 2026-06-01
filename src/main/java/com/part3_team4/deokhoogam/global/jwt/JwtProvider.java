package com.part3_team4.deokhoogam.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
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

  public String createAccessToken(String email) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + accessTokenExpTime);

    return Jwts.builder()
        .subject(email)
        .issuedAt(now)
        .expiration(expiration)
        .signWith(key)
        .compact();
  }
}
