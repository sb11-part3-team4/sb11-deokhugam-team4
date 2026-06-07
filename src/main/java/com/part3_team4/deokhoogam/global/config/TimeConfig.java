package com.part3_team4.deokhoogam.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 관련 Bean 설정입니다.
 *
 * Clock을 Bean으로 등록해두면,
 * 운영 환경에서는 실제 UTC 시간을 사용하고
 * 테스트에서는 고정된 Clock을 주입해서 시간 계산을 안정적으로 검증할 수 있습니다.
 */
@Configuration
public class TimeConfig {

  /**
   * 운영 환경에서 사용할 Clock Bean입니다.
   *
   * UTC 기준 시간을 사용하면 서버의 로컬 타임존에 영향을 덜 받습니다.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}