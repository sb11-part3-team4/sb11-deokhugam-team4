package com.part3_team4.deokhoogam.domain.book.Factory;

import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BookFixtureFactory {

  public static BookDto createMobyDick(UUID mockId) {
    Instant at = Instant.now().minusSeconds(10);
    return BookDto.builder()
        .id(mockId)
        .title("모비 딕")
        .author("허먼 멜빌")
        .description("『모비 딕』 완역본")
        .publisher("작가정신")
        .publishedDate(LocalDate.of(2024, 4, 9))
        .thumbnailUrl("temp/url/moby-dick")
        .reviewCount(2)
        .rating(BigDecimal.valueOf(4.5))
        .isbn("9791160263404")
        .createdAt(at)
        .updatedAt(at)
        .build();
  }


  public static BookDto createJavaStandard(UUID mockId) {
    Instant at = Instant.now().minusSeconds(60);
    return BookDto.builder()
        .id(mockId)
        .title("자바의 정석")
        .author("남궁성")
        .description("자바 프로그래밍의 바이블")
        .publisher("도우출판")
        .publishedDate(LocalDate.of(2016, 1, 27))
        .thumbnailUrl("temp/url/java-standard")
        .reviewCount(150)
        .rating(BigDecimal.valueOf(4.8))
        .isbn("9788994492032")
        .createdAt(at)
        .updatedAt(at)
        .build();
  }


  public static BookDto createCleanCode(UUID mockId) {
    Instant at = Instant.now().minusSeconds(120);
    return BookDto.builder()
        .id(mockId)
        .title("클린 코드")
        .author("로버트 C. 마틴")
        .description("애자일 소프트웨어 장인 정신")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2013, 12, 24))
        .thumbnailUrl("temp/url/clean-code")
        .reviewCount(320)
        .rating(BigDecimal.valueOf(4.9))
        .isbn("9788966260959")
        .createdAt(at)
        .updatedAt(at)
        .build();
  }

  public static BookDto createLittlePrince(UUID mockId) {
    Instant at = Instant.now().minusSeconds(300);
    return BookDto.builder()
        .id(mockId)
        .title("어린 왕자")
        .author("앙투안 드 생텍쥐페리")
        .description("어른들을 위한 동화")
        .publisher("열린책들")
        .publishedDate(LocalDate.of(2015, 10, 20))
        .thumbnailUrl("temp/url/little-prince")
        .reviewCount(85)
        .rating(BigDecimal.valueOf(4.7))
        .isbn("9788932917245")
        .createdAt(at)
        .updatedAt(at)
        .build();
  }

  public static Book createBook1() {
    return Book.builder()
        .title("모비 딕")
        .author("허먼 멜빌")
        .description("『모비 딕』 완역본")
        .publisher("작가정신")
        .publishedDate(LocalDate.of(2024, 4, 9))
        .thumbnailUrl("temp/url/moby-dick")
        .isbn("9791160263404")
        .build();
  }
  public static Book createBook2(){
    return Book.builder()
        .title("자바의 정석")
        .author("남궁성")
        .description("자바 프로그래밍의 바이블")
        .publisher("도우출판")
        .publishedDate(LocalDate.of(2016, 1, 27))
        .thumbnailUrl("temp/url/java-standard")
        .isbn("9788994492032")
        .build();
  }
  public static Book createBook3(){
    return Book.builder()
        .title("클린 코드")
        .author("로버트 C. 마틴")
        .description("애자일 소프트웨어 장인 정신")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2013, 12, 24))
        .thumbnailUrl("temp/url/clean-code")
        .isbn("9788966260959")
        .build();
  }
  public static Book createBook4(){
    return Book.builder()
        .title("어린 왕자")
        .author("앙투안 드 생텍쥐페리")
        .description("어른들을 위한 동화")
        .publisher("열린책들")
        .publishedDate(LocalDate.of(2015, 10, 20))
        .thumbnailUrl("temp/url/little-prince")
        .isbn("9788932917245")
        .build();
  }


  public static List<BookDto> createBookDtoList() {
    return List.of(
        createMobyDick(UUID.randomUUID()),
        createJavaStandard(UUID.randomUUID()),
        createCleanCode(UUID.randomUUID()),
        createLittlePrince(UUID.randomUUID())
    );
  }
  public static List<Book> createBookList() {
    return List.of(
        createBook1(),
        createBook4(),
        createBook2(),
        createBook3()
    );
  }
}