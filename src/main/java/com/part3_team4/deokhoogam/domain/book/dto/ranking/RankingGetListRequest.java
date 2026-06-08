package com.part3_team4.deokhoogam.domain.book.dto.ranking;

import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RankingGetListRequest(
    PeriodType period,
    Direction direction,
    String cursor,
    @Min(1)
    @Max(100)
    int limit
) {}