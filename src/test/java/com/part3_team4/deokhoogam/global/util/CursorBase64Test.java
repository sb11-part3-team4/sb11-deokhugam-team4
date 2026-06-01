package com.part3_team4.deokhoogam.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorBase64Test {

  @Test
  @DisplayName("커서 객체를 Base64로 인코딩하고 다시 디코딩하면 원래 데이터와 일치해야 한다")
  void encode_and_decode_cursor() {
    // given: 테스트할 커서 객체 생성
    BookCursor originalCursor = new BookCursor("4.5", UUID.randomUUID(), "2023-10-15T10:00:00");
    UUID id = originalCursor.getId();


    // when: 인코딩 -> 디코딩 진행
    String encoded = CursorUtils.encodeCursor(originalCursor);
    BookCursor decodedCursor = CursorUtils.decodeCursor(encoded);

    // then: 알 수 없는 문자열이 생성되었는지 확인
    assertThat(encoded).isNotBlank();

    // then: 디코딩된 객체의 값이 원래 값과 정확히 일치하는지 확인
    assertThat(decodedCursor.getMainValue()).isEqualTo("4.5");
    assertThat(decodedCursor.getId()).isEqualTo(id);
    assertThat(decodedCursor.getCreatedAt()).isEqualTo("2023-10-15T10:00:00");
  }
}