package com.part3_team4.deokhoogam.batch.BookRanking;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class RankingPeriod {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final Instant start;
  private final Instant end;

  private RankingPeriod(Instant start, Instant end) {
    this.start = start;
    this.end = end;
  }

  // period 를 받아 (시작, 끝) 을 계산
  public static RankingPeriod of(PeriodType period) {
    Instant now = Instant.now();                              // 끝은 항상 지금
    LocalDate today = LocalDate.now(KST);                     // 한국 기준 오늘 날짜

    Instant start = switch (period) {
      case DAILY    -> today.atStartOfDay(KST).toInstant();                  // 오늘 0시(KST)
      case WEEKLY   -> today.minusDays(6).atStartOfDay(KST).toInstant();     // 6일 전 0시 ~ = 오늘 포함 7일
      case MONTHLY  -> today.minusDays(29).atStartOfDay(KST).toInstant();    // 29일 전 0시 ~ = 30일
      case ALL_TIME -> Instant.EPOCH;                                        // 전체 (1970~)
    };

    return new RankingPeriod(start, now);
  }
}