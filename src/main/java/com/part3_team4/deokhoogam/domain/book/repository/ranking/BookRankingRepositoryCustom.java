package com.part3_team4.deokhoogam.domain.book.repository.ranking;

import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import org.springframework.data.domain.Slice;

public interface BookRankingRepositoryCustom {
  Slice<BookRanking> getRankings(PeriodType period, Direction direction, Integer cursor, int limit);
}