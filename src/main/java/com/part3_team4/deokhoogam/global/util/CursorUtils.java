package com.part3_team4.deokhoogam.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.RankingCursor;
import com.part3_team4.deokhoogam.global.exception.Base64Exception;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorUtils {

  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 디코딩 오류 방지
  }

  // 인코딩 메서드
  public static String encodeCursor(BookCursor cursor) {
    if (cursor == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(cursor);
      return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw Base64Exception.EncodingError();
    }
  }

  // 디코딩 메서드
  public static BookCursor decodeCursor(String base64Cursor) {
    if (base64Cursor == null || base64Cursor.isBlank()) {
      return null;
    }
    try {
      byte[] decodedBytes = Base64.getDecoder().decode(base64Cursor);
      String json = new String(decodedBytes, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, BookCursor.class);
    } catch (Exception e) {
      throw Base64Exception.DecodingError();
    }
  }

  public static String encodeRankingCursor(RankingCursor cursor) {
    if (cursor == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(cursor);
      return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw Base64Exception.EncodingError();
    }
  }

  public static RankingCursor decodeRankingCursor(String base64Cursor) {
    if (base64Cursor == null || base64Cursor.isBlank()) {
      return null;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(base64Cursor);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, RankingCursor.class);
    } catch (Exception e) {
      throw Base64Exception.DecodingError();
    }
  }

}