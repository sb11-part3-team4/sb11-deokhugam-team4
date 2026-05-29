package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("BookRepository 테스트")
class BookRepositoryTest {

  @Autowired
  private BookRepository bookRepository;

  @Test
  @DisplayName("ISBN 존재 여부를 확인한다")
  void existsByIsbn_WhenBookExists_ReturnsTrue() {
    // given
    String targetIsbn = "1234567890123";
    Book book = createFixtureBook(targetIsbn);
    bookRepository.save(book);

    // when
    boolean exists = bookRepository.existsByIsbn(targetIsbn);
    boolean notExists = bookRepository.existsByIsbn("0000000000000");

    // then
    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  // 픽스처 메서드
  private Book createFixtureBook(String isbn) {
    return Book.builder()
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 프로그래밍 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .isbn(isbn)
        .build();
  }
}