package com.part3_team4.deokhoogam.domain.book.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookRankingDto;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.RankingCursor;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.ranking.BookRankingRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookRankingService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookRankingServiceTest {

  @Mock
  private BookRankingRepository bookRankingRepository;
  @Mock
  private BookRepository bookRepository;

  @InjectMocks
  private BookRankingService bookRankingService;

  // 북 랭킹 만드는 메서드
  private BookRanking ranking(UUID id, UUID bookId, int rank) {
    BookRanking r = BookRanking.builder()
        .bookId(bookId)
        .period(PeriodType.DAILY)
        .score(new BigDecimal("4.5"))
        .ranking(rank)
        .reviewCount(10)
        .rating(new BigDecimal("4.50"))
        .build();
    ReflectionTestUtils.setField(r, "id", id);
    return r;
  }

  private Book book(UUID id, String title) {
    Book b = Book.builder()
        .title(title).author("저자").description("설명").publisher("출판사")
        .publishedDate(LocalDate.of(2020, 1, 1)).isbn("ISBN" + id).thumbnailUrl("url/" + title)
        .build();
    ReflectionTestUtils.setField(b, "id", id);
    return b;
  }

  @Test
  @DisplayName("랭킹과 책 정보를 합쳐 응답을 만든다")
  void getRankings_basic() {
    UUID bookId = UUID.randomUUID();
    BookRanking r = ranking(UUID.randomUUID(), bookId, 1);
    Slice<BookRanking> slice = new SliceImpl<>(List.of(r), PageRequest.of(0, 10), false);

    when(bookRankingRepository.getRankings(eq(PeriodType.DAILY), any(Direction.class), isNull(), eq(10)))
        .thenReturn(slice);
    when(bookRepository.findAllById(any()))
        .thenReturn(List.of(book(bookId, "모비 딕")));

    PageResponse<BookRankingDto> response =
        bookRankingService.getRankings(PeriodType.DAILY, Direction.ASC,null, 10);

    assertThat(response.content()).hasSize(1);
    BookRankingDto dto = response.content().get(0);
    assertThat(dto.bookId()).isEqualTo(bookId);
    assertThat(dto.title()).isEqualTo("모비 딕");      // Book 에서 합쳐짐
    assertThat(dto.rank()).isEqualTo(1);
    assertThat(dto.score()).isEqualByComparingTo("4.5"); // BookRanking 에서
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();          // 다음 페이지 없으면 null
  }

  @Test
  @DisplayName("다음 페이지가 있으면 마지막 ranking으로 nextCursor를 만든다")
  void getRankings_nextCursor() {
    UUID bookId1 = UUID.randomUUID();
    UUID bookId2 = UUID.randomUUID();
    BookRanking r1 = ranking(UUID.randomUUID(), bookId1, 1);
    BookRanking r2 = ranking(UUID.randomUUID(), bookId2, 2);
    Slice<BookRanking> slice = new SliceImpl<>(List.of(r1, r2), PageRequest.of(0, 2), true);

    when(bookRankingRepository.getRankings(eq(PeriodType.DAILY), any(Direction.class), isNull(), eq(2)))
        .thenReturn(slice);
    when(bookRepository.findAllById(any()))
        .thenReturn(List.of(book(bookId1, "책1"), book(bookId2, "책2")));

    PageResponse<BookRankingDto> response =
        bookRankingService.getRankings(PeriodType.DAILY, Direction.ASC, null, 2);

    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
    // nextCursor 를 디코딩하면 마지막 ranking(2)이 들어있어야 한다
    RankingCursor decoded = CursorUtils.decodeRankingCursor(response.nextCursor());
    assertThat(decoded.getRanking()).isEqualTo(2);
  }

  @Test
  @DisplayName("커서 문자열을 디코딩해 ranking 값으로 리포지토리를 호출한다")
  void getRankings_decodeCursor() {
    // ranking=5 를 인코딩한 커서를 입력으로 준다
    String cursor = CursorUtils.encodeRankingCursor(
        RankingCursor.builder().ranking(5).build());

    Slice<BookRanking> empty = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);
    when(bookRankingRepository.getRankings(eq(PeriodType.DAILY), any(Direction.class), eq(5), eq(10)))
        .thenReturn(empty);
    when(bookRepository.findAllById(any())).thenReturn(List.of());

    bookRankingService.getRankings(PeriodType.DAILY, Direction.ASC, cursor, 10);

    // 위 when 의 eq(5) 가 매칭됐다는 것 = 디코딩된 5로 호출했다는 것
    // (매칭 안 되면 NPE 나거나 빈 stub 반환되어 검증 실패)
  }
}
