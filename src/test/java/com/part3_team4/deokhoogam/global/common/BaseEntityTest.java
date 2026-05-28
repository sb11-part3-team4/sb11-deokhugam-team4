package com.part3_team4.deokhoogam.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BaseEntityTest {

  // 초반 테스트 커버리지 채우기 용
  @Test
  void test() {

    BaseEntity baseEntity = new BaseEntity();

    assertThat(baseEntity.getId()).isNotNull();
    assertThat(baseEntity.getCreatedAt()).isNull();
    assertThat(baseEntity.getUpdatedAt()).isNull();

  }
}
