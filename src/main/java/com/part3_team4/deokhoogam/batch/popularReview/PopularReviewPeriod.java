package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@Getter
public class PopularReviewPeriod {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Instant start;
    private final Instant end;

    private PopularReviewPeriod(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public static PopularReviewPeriod of(PeriodType period) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(KST);

        Instant start = switch (period) {
            case DAILY -> today.atStartOfDay(KST).toInstant();
            case WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    .atStartOfDay(KST).toInstant();
            case MONTHLY -> today.withDayOfMonth(1).atStartOfDay(KST).toInstant();
            case ALL_TIME -> Instant.EPOCH;
        };

        return new PopularReviewPeriod(start, now);
    }

}
