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

  private static final  ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 디코딩 오류 방지
  }

  private static <T> String encode(T cursor) {
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

  private static <T> T decode(String base64Cursor, Class<T> type) {
    if (base64Cursor == null || base64Cursor.isBlank()) {
      return null;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(base64Cursor);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, type);
    } catch (Exception e) {
      throw Base64Exception.DecodingError();
    }
  }

  public static String encodeCursor(BookCursor cursor) {
    return encode(cursor);
  }

  public static BookCursor decodeCursor(String base64Cursor) {
    return decode(base64Cursor, BookCursor.class);
  }

  public static String encodeRankingCursor(RankingCursor cursor) {
    return encode(cursor);
  }

  public static RankingCursor decodeRankingCursor(String base64Cursor) {
    return decode(base64Cursor, RankingCursor.class);
  }

}