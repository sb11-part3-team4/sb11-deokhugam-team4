package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;


public class PopularReviewPeriodTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("DAILY는 오늘 0시(KST)를 start로 반환한다")
    void daily_start_is_today_midnight() {
        PopularReviewPeriod period = PopularReviewPeriod.of(PeriodType.DAILY);
        Instant expected = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        assertThat(period.getStart()).isEqualTo(expected);
    }

    @Test
    @DisplayName("WEEKLY는 이번 주 일요일 0시(KST)를 start로 반환한다")
    void weekly_start_is_this_sunday_midnight() {
        PopularReviewPeriod period = PopularReviewPeriod.of(PeriodType.WEEKLY);
        Instant expected = LocalDate.now(KST)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .atStartOfDay(KST).toInstant();
        assertThat(period.getStart()).isEqualTo(expected);
    }

    @Test
    @DisplayName("MONTHLY는 이번 달 1일 0시(KST)를 start로 반환한다")
    void monthly_start_is_first_day_of_month() {
        PopularReviewPeriod period = PopularReviewPeriod.of(PeriodType.MONTHLY);
        Instant expected = LocalDate.now(KST).withDayOfMonth(1).atStartOfDay(KST).toInstant();
        assertThat(period.getStart()).isEqualTo(expected);
    }

    @Test
    @DisplayName("ALL_TIME은 Instant.EPOCH를 start로 반환한다")
    void allTime_start_is_epoch(){
        PopularReviewPeriod period = PopularReviewPeriod.of(PeriodType.ALL_TIME);
        assertThat(period.getStart()).isEqualTo(Instant.EPOCH);
    }
}
