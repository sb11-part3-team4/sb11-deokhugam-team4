package com.part3_team4.deokhoogam.domain.ranking.repository;

import com.part3_team4.deokhoogam.domain.ranking.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.ranking.entity.PeriodType;
import org.springframework.data.domain.Slice;

public interface BookRankingRepositoryCustom {
  Slice<BookRanking> getRankings(PeriodType period, Integer cursor, int limit);
}