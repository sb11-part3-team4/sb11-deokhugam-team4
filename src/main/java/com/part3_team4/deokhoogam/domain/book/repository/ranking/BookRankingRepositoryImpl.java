package com.part3_team4.deokhoogam.domain.book.repository.ranking;

import static com.part3_team4.deokhoogam.domain.book.entity.QBookRanking.bookRanking;

import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class BookRankingRepositoryImpl implements BookRankingRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<BookRanking> getRankings(PeriodType period, Direction direction,
      Integer cursor, int limit) {

    boolean isAsc = (direction == null || direction == Direction.ASC);
    PeriodType targetPeriod = (period == null) ? PeriodType.DAILY : period;

    OrderSpecifier<Integer> order = isAsc
        ? bookRanking.ranking.asc()
        : bookRanking.ranking.desc();

    List<BookRanking> content = queryFactory
        .selectFrom(bookRanking)
        .where(
            bookRanking.period.eq(targetPeriod),
            cursorCondition(cursor, isAsc)
        )
        .orderBy(order)
        .limit(limit + 1)
        .fetch();

    boolean hasNext = content.size() > limit;
    if (hasNext) {
      content.remove(limit);
    }
    return new SliceImpl<>(content, PageRequest.of(0, limit), hasNext);
  }

  private BooleanExpression cursorCondition(Integer cursor, boolean isAsc) {
    if (cursor == null) {
      return null;
    }
    return isAsc ? bookRanking.ranking.gt(cursor) : bookRanking.ranking.lt(cursor);
  }
}