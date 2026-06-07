package com.part3_team4.deokhoogam.global.fixture;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public final class BookFixtures {

  private BookFixtures() {
  }

  public static BookCreateRequest validBookCreateRequest() {
    return BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();
  }

  public static BookUpdateRequest validBookUpdateRequest() {
    return BookUpdateRequest.builder()
        .title("클린 아키텍처")
        .author("로버트 C. 마틴")
        .description("소프트웨어 구조와 설계의 원칙")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 29))
        .build();
  }

  public static Book validBook(String isbn) {
    return Book.builder()
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 프로그래밍 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .isbn(isbn)
        .build();
  }


  public static Book validBookToId(UUID mockId) {

    Instant createdAt = Instant.now().minusSeconds(100);
    Instant updatedAt = Instant.now().minusSeconds(50);

    Book book = Book.builder()
        .title("모비 딕")
        .author("허먼 멜빌")
        .description("『모비 딕』 완역본")
        .publisher("작가정신")
        .publishedDate(LocalDate.of(2024, 4, 9))
        .thumbnailUrl("temp/url")
        .isbn("9791160263404")
        .build();

    ReflectionTestUtils.setField(book, "id", mockId);
    ReflectionTestUtils.setField(book, "createdAt", createdAt);
    ReflectionTestUtils.setField(book, "updatedAt", updatedAt);

    return book;

  }

  public static DeletedBook deletedBook(UUID mockId) {

    Instant createdAt = Instant.now().minusSeconds(100);
    Instant updatedAt = Instant.now().minusSeconds(50);

    DeletedBook book = DeletedBook.builder()
        .title("모비 딕")
        .author("허먼 멜빌")
        .description("『모비 딕』 완역본")
        .publisher("작가정신")
        .publishedDate(LocalDate.of(2024, 4, 9))
        .thumbnailUrl("temp/url")
        .isbn("9791160263404")
        .build();

    ReflectionTestUtils.setField(book, "id", mockId);
    ReflectionTestUtils.setField(book, "createdAt", createdAt);
    ReflectionTestUtils.setField(book, "updatedAt", updatedAt);
    ReflectionTestUtils.setField(book, "deletedAt", Instant.now());


    return book;
  }
}