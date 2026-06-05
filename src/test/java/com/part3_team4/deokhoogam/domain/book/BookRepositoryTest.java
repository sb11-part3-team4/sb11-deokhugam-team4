package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.book.Factory.BookFixtureFactory;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.support.RepositoryTestSupport;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("BookRepository 테스트")
class BookRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private BookRepository bookRepository;

  @Test
  @DisplayName("저장된 ISBN으로 조회 시 true를 반환한다")
  void existsByIsbn_savedIsbn_returnsTrue() {
    // given
    String targetIsbn = "1234567890123";
    Book book = BookFixtures.validBook(targetIsbn);
    bookRepository.save(book);

    // when
    boolean exists = bookRepository.existsByIsbn(targetIsbn);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("저장되지 않은 ISBN으로 조회 시 false를 반환한다")
  void existsByIsbn_notSavedIsbn_returnsFalse() {
    // given
    String notSavedIsbn = "0000000000000";

    // when
    boolean notExists = bookRepository.existsByIsbn(notSavedIsbn);

    // then
    assertThat(notExists).isFalse();
  }


  @DataJpaTest
  @DisplayName("BookRepository 커서 페이지네이션 에서")
  @Nested
  class TestCursorPagination {


    @BeforeEach
    void setUp() {
      bookRepository.deleteAll();

      List<Book> savedBooks = BookFixtureFactory.createBookList();
      bookRepository.saveAll(savedBooks);

    }


    @Test
    @DisplayName("커서가 없으면 최신 데이터부터 limit만큼 가져온다")
    void getBooks_firstPage() {
      // given
      BookGetListRequest request = new BookGetListRequest("", SortType.TITLE, Direction.ASC,
          null, null, 2);
      BookCursor cursor = null; // 첫 요청

      List<Book> savedBooks = BookFixtureFactory.createBookList();

      // when
      Slice<Book> result = bookRepository.getBooks(cursor, request);

      // then
      assertThat(result.getContent()).hasSize(2); // limit 개수만큼 가져왔는가?
      assertThat(result.hasNext()).isTrue(); // 다음 페이지가 있는가?

      // 정렬이 최신순으로 잘 되었는가? (미리 저장해둔 최신순 리스트의 0, 1번 인덱스와 비교)
      assertThat(result.getContent().get(0).getTitle()).isEqualTo(savedBooks.get(0).getTitle());
      assertThat(result.getContent().get(1).getTitle()).isEqualTo(savedBooks.get(1).getTitle());
    }

    @Test
    @DisplayName("커서 값 이후의 데이터를 정상적으로 가져오고, 마지막 페이지일시 hasnext = fasle를 반환한다")
    void getBooks_nextPage() {

      List<Book> savedBooks = bookRepository.findAll();

      savedBooks.sort(
          Comparator.comparing(Book::getTitle));




      Book lastBookOfFirstPage = savedBooks.get(1);

      BookCursor testCursor = new BookCursor( //커서.
          lastBookOfFirstPage.getTitle(),
          lastBookOfFirstPage.getId(),
          lastBookOfFirstPage.getCreatedAt().truncatedTo(ChronoUnit.MILLIS)
      );

      String cursor = CursorUtils.encodeCursor(testCursor); //인코딩

      BookGetListRequest request = new BookGetListRequest("", SortType.TITLE, Direction.ASC,
          cursor, lastBookOfFirstPage.getCreatedAt(), 2);

      // when
      Slice<Book> result = bookRepository.getBooks(testCursor, request);

      // then
      assertThat(result.getContent()).hasSize(2); // 총 네개. 두개를 가져오고 두개를 추가로 가져온 상황
      assertThat(result.hasNext()).isFalse(); //


      assertThat(result.getContent().get(0).getId()).isEqualTo(savedBooks.get(2).getId());
      assertThat(result.getContent().get(1).getId()).isEqualTo(savedBooks.get(3).getId());
    }


  }

}