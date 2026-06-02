package com.part3_team4.deokhoogam.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResponseTest {

  @Test
  @DisplayName("커서 페이지네이션 응답 값을 보관한다")
  void createPageResponse() {
    PageResponse<Integer> page = new PageResponse<>(
        List.of(1, 2, 3, 4, 5),
        "nextCursor",
        "2026-06-02T00:00:00Z",
        5,
        10L,
        true
    );

    assertThat(page.content()).containsExactly(1, 2, 3, 4, 5);
    assertThat(page.nextCursor()).isEqualTo("nextCursor");
    assertThat(page.nextAfter()).isEqualTo("2026-06-02T00:00:00Z");
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(10L);
    assertThat(page.hasNext()).isTrue();
  }
}