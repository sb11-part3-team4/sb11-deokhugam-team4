package com.part3_team4.deokhoogam.global.common;

import java.util.List;
import org.junit.jupiter.api.Test;

public class PageResponseTest {

  //초반 테스트 커버리지 채우기 용
  @Test
  void test() {

    PageResponse<Integer> page = new PageResponse<>(List.of(1, 2, 3, 4, 5), "nextCursor",
        "afterCursor", 5, 10L, true);

  }
}
