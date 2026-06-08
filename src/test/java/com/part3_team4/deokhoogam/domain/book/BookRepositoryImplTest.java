package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import com.part3_team4.deokhoogam.global.fixture.BookRepositoryImplFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@ActiveProfiles("test")
@DisplayName("BookRepositoryImpl용 테스트")
class BookRepositoryImplTest {

  @Autowired
  private BookRepository bookRepository;
  @Autowired
  private EntityManager em;


  // request 만들기
  private BookGetListRequest request(String keyword, SortType orderBy, Direction direction,
      int limit) {
    return new BookGetListRequest(keyword, orderBy, direction, null, null, limit); //리포지토리에 들어 따로 커서 들어감.
  }

  // 커서 만들기
  private BookCursor cursorOf(Book b, SortType sort) {
    String mainValue = switch (sort) {
      case TITLE -> b.getTitle();
      case RATING -> b.getRating().toString();
      case REVIEW_COUNT -> String.valueOf(b.getReviewCount());
      case PUBLISHED_DATE -> b.getPublishedDate().toString();
    };
    return new BookCursor(
        mainValue,
        b.getId(),
        b.getCreatedAt()
        );
  }
// 책 제목 가져오기 (제대로 가져왔는지 비교용)
  private List<String> titlesOf(Slice<Book> slice) {
    return slice.getContent().stream().map(Book::getTitle).toList();
  }

  // ── 첫 페이지 / hasNext ──

  @Nested
  @DisplayName("첫 페이지 테스트에서 ")
  class FirstPage {

    @Test
    @DisplayName("커서가 null이면 처음부터 조회된다")
    void cursorNull() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request(null, SortType.TITLE, Direction.ASC, 5));

      assertThat(titlesOf(result)).containsExactly("1984", "데미안", "동물농장", "모던 자바 인 액션", "모비 딕");
      assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("커서 id가 null이면 첫 페이지처럼 동작한다")
    void cursorIdNull() {
      BookRepositoryImplFixture.createBooks(em);
      BookCursor emptyCursor = BookCursor.builder().id(null).build();

      Slice<Book> result = bookRepository.getBooks(
          emptyCursor, request(null, SortType.TITLE, Direction.ASC, 3));

      assertThat(titlesOf(result)).containsExactly("1984", "데미안", "동물농장");
    }

    @Test
    @DisplayName("결과가 limit보다 많으면 hasNext=true, limit 개만 반환한다")
    void hasNextTrue() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request(null, SortType.TITLE, Direction.ASC, 3));

      assertThat(result.getContent()).hasSize(3);
      assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("결과가 limit 이하면 hasNext=false를 반환한다")
    void hasNextFalse() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request(null, SortType.TITLE, Direction.ASC, 20));

      assertThat(result.getContent()).hasSize(14);
      assertThat(result.hasNext()).isFalse();
    }
  }

  // ── 키워드 검색 ──

  @Nested
  @DisplayName("키워드 검색 테스트에서")
  class Keyword {

    @Test
    @DisplayName("제목으로 검색된다")
    void byTitle() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request("클린", SortType.TITLE, Direction.ASC, 10));

      assertThat(titlesOf(result)).contains("클린 코드");
      assertThat(titlesOf(result)).contains("클린 아키텍처");
    }

    @Test
    @DisplayName("저자로 검색된다")
    void byAuthor() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request("남궁성", SortType.TITLE, Direction.ASC, 10));

      assertThat(titlesOf(result)).containsExactly("자바의 정석");
    }

    @Test
    @DisplayName("ISBN으로 검색된다")
    void byIsbn() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request("9788932917245", SortType.TITLE, Direction.ASC, 10));

      assertThat(titlesOf(result)).containsExactly("어린 왕자");
    }

    @Test
    @DisplayName("키워드가 공백이면 전체 조회된다")
    void blankKeyword() {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> result = bookRepository.getBooks(
          null, request("   ", SortType.TITLE, Direction.ASC, 10));

      assertThat(result.getContent()).hasSize(10);
    }
  }

  // ── 정렬 4종 × 방향 2종 × 커서 ──

  @Nested
  @DisplayName("정렬/방향별 커서 페이징 테스트에서")
  class SortAndCursor {

    static Stream<Arguments> sortDirections() {
      return Stream.of(
          Arguments.of(SortType.TITLE, Direction.ASC),
          Arguments.of(SortType.TITLE, Direction.DESC),
          Arguments.of(SortType.RATING, Direction.ASC),
          Arguments.of(SortType.RATING, Direction.DESC),
          Arguments.of(SortType.REVIEW_COUNT, Direction.ASC),
          Arguments.of(SortType.REVIEW_COUNT, Direction.DESC),
          Arguments.of(SortType.PUBLISHED_DATE, Direction.ASC),
          Arguments.of(SortType.PUBLISHED_DATE, Direction.DESC)
      );
    }


    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("sortDirections")
    @DisplayName("커서 다음 페이지가 정렬 순서대로 중복 없이 이어진다")
    void cursorPaging(SortType sort, Direction direction) {
      BookRepositoryImplFixture.createBooks(em);

      Slice<Book> page1 = bookRepository.getBooks(null, request(null, sort, direction, 3));
      assertThat(titlesOf(page1)).containsExactlyElementsOf(expectedPage1(sort, direction));
      assertThat(page1.hasNext()).isTrue();

      Book last = page1.getContent().get(page1.getContent().size() - 1);
      Slice<Book> page2 = bookRepository.getBooks(cursorOf(last, sort),
          request(null, sort, direction, 3));
      assertThat(titlesOf(page2)).containsExactlyElementsOf(expectedPage2(sort, direction));
      assertThat(titlesOf(page2)).doesNotContain(last.getTitle());
    }

    // page1 = 정렬 기준 1~3등
    private List<String> expectedPage1(SortType sort, Direction direction) {
      boolean asc = direction == Direction.ASC;
      return switch (sort) {
        case TITLE -> asc
            ? List.of("1984", "데미안", "동물농장")
            : List.of("호모 데우스", "토비의 스프링", "클린 코드");
        case RATING -> asc
            ? List.of("동물농장", "1984", "호모 데우스")
            : List.of("토비의 스프링", "이펙티브 자바", "클린 코드");
        case REVIEW_COUNT -> asc
            ? List.of("모비 딕", "어린 왕자", "동물농장")
            : List.of("클린 코드", "이펙티브 자바", "자바의 정석");
        case PUBLISHED_DATE -> asc
            ? List.of("동물농장", "1984", "코스모스")
            : List.of("모비 딕", "클린 아키텍처", "모던 자바 인 액션");
      };
    }

    // page2 = 정렬 기준 4~6등
    private List<String> expectedPage2(SortType sort, Direction direction) {
      boolean asc = direction == Direction.ASC;
      return switch (sort) {
        case TITLE -> asc
            ? List.of("모던 자바 인 액션", "모비 딕", "사피엔스")
            : List.of("클린 아키텍처", "코스모스", "자바의 정석");
        case RATING -> asc
            ? List.of("데미안", "어린 왕자", "모비 딕")
            : List.of("자바의 정석", "클린 아키텍처", "코스모스");
        case REVIEW_COUNT -> asc
            ? List.of("1984", "데미안", "호모 데우스")   // ⚠️ 정상 기준값 — 현재 구현은 버그로 실패
            : List.of("클린 아키텍처", "사피엔스", "토비의 스프링");
        case PUBLISHED_DATE -> asc
            ? List.of("데미안", "토비의 스프링", "클린 코드")
            : List.of("이펙티브 자바", "호모 데우스", "자바의 정석");
      };
    }
  }

}
