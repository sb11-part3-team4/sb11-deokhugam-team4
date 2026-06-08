package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookRankingDto;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.RankingCursor;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.ranking.BookRankingRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookRankingService {

  private final BookRankingRepository bookRankingRepository;
  private final BookRepository bookRepository;

  public PageResponse<BookRankingDto> getRankings(PeriodType period, Direction direction,String cursor, int limit) {
    // 커서 디코딩
    RankingCursor decoded = CursorUtils.decodeRankingCursor(cursor);
    Integer rankingCursor = (decoded == null) ? null : decoded.getRanking();



    // 랭킹 슬라이스 조회
    Slice<BookRanking> slice = bookRankingRepository.getRankings(period, direction,rankingCursor, limit);
    List<BookRanking> rankings = slice.getContent();

    // 책 정보 한 번에 조회 (N+1 방지)
    List<UUID> bookIds = rankings.stream().map(BookRanking::getBookId).toList();
    Map<UUID, Book> bookMap = bookRepository.findAllById(bookIds).stream()
        .collect(Collectors.toMap(Book::getId, Function.identity()));

    // 랭킹 + 책 정보 합쳐 DTO 조립
    List<BookRankingDto> content = rankings.stream()
        .map(r -> toDto(r, bookMap.get(r.getBookId())))
        .toList();

    // 5. 다음 페이지 있으면 커서 인코딩
    String nextCursor = null;
    if (slice.hasNext() && !rankings.isEmpty()) {
      int lastRanking = rankings.get(rankings.size() - 1).getRanking();
      nextCursor = CursorUtils.encodeRankingCursor(
          RankingCursor.builder().ranking(lastRanking).build());
    }

    return new PageResponse<>(
        content,
        nextCursor,
        null,                 // nextAfter 미사용
        content.size(),
        null,                 // totalElements 미사용 (슬라이스라 전체개수 없음)
        slice.hasNext()
    );
  }

  private BookRankingDto toDto(BookRanking r, Book book) {
    return new BookRankingDto(
        r.getId(),
        r.getBookId(),
        book != null ? book.getTitle() : null,
        book != null ? book.getAuthor() : null,
        book != null ? book.getThumbnailUrl() : null,
        r.getPeriod().name(),
        r.getRanking(),
        r.getScore(),
        r.getReviewCount(),
        r.getRating(),
        r.getCreatedAt()
    );
  }
}
