package com.part3_team4.deokhoogam.domain.ranking.repository;

import static com.part3_team4.deokhoogam.domain.ranking.entity.QBookRanking.bookRanking;

import com.part3_team4.deokhoogam.domain.ranking.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.ranking.entity.PeriodType;
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
  public Slice<BookRanking> getRankings(PeriodType period, Integer cursor, int limit) {
    List<BookRanking> content = queryFactory
        .selectFrom(bookRanking)
        .where(
            bookRanking.period.eq(period),   // 기간 필터
            cursorCondition(cursor)          // 커서: ranking 다음부터
        )
        .orderBy(bookRanking.ranking.asc())  // 순위 오름차순
        .limit(limit + 1)                    // hasNext 판단용 +1
        .fetch();

    boolean hasNext = content.size() > limit;
    if (hasNext) {
      content.remove(limit);
    }
    return new SliceImpl<>(content, PageRequest.of(0, limit), hasNext);
  }

  // 커서가 null이면 첫 페이지(조건 없음), 있으면 그 ranking보다 큰 것부터
  private BooleanExpression cursorCondition(Integer cursor) {
    if (cursor == null) {
      return null;
    }
    return bookRanking.ranking.gt(cursor);
  }
}
