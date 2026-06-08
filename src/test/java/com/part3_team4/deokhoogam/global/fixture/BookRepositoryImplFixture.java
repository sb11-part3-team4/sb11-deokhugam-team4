package com.part3_team4.deokhoogam.global.fixture;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//BookRepositoryImpl 테스트 용
public class BookRepositoryImplFixture {

  private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");

  private BookRepositoryImplFixture() {
  }

  // 총 14개
  public static List<Book> createBooks(EntityManager em) {
    List<Book> books = new ArrayList<>();
    books.add(save(em, "클린 코드", "로버트 C. 마틴", "9788966260959",
        bd(4.9), 320, date(2013, 12, 24), 10));
    books.add(save(em, "클린 아키텍처", "로버트 C. 마틴", "9791162242179",
        bd(4.8), 210, date(2019, 8, 20), 20));
    books.add(save(em, "이펙티브 자바", "조슈아 블로크", "9788966262281",
        bd(4.9), 280, date(2018, 11, 1), 30));
    books.add(save(em, "자바의 정석", "남궁성", "9788994492032",
        bd(4.8), 210, date(2016, 1, 27), 40));
    books.add(save(em, "모던 자바 인 액션", "라울-게이브리얼 우르마", "9791162242025",
        bd(4.7), 150, date(2019, 1, 15), 50));
    books.add(save(em, "토비의 스프링", "이일민", "9788960773417",
        bd(4.9), 180, date(2012, 9, 10), 60));
    books.add(save(em, "모비 딕", "허먼 멜빌", "9791160263404",
        bd(4.5), 30, date(2024, 4, 9), 70));
    books.add(save(em, "어린 왕자", "앙투안 드 생텍쥐페리", "9788932917245",
        bd(4.5), 85, date(2015, 10, 20), 80));
    books.add(save(em, "데미안", "헤르만 헤세", "9788937460449",
        bd(4.5), 120, date(2009, 1, 1), 90));
    books.add(save(em, "1984", "조지 오웰", "9788937460777",
        bd(4.3), 95, date(2003, 6, 1), 100));
    books.add(save(em, "동물농장", "조지 오웰", "9788937462788",
        bd(4.3), 95, date(2003, 6, 1), 110));
    books.add(save(em, "사피엔스", "유발 하라리", "9788934972464",
        bd(4.6), 200, date(2015, 11, 24), 120));
    books.add(save(em, "호모 데우스", "유발 하라리", "9788934986997",
        bd(4.4), 130, date(2017, 5, 19), 130));
    books.add(save(em, "코스모스", "칼 세이건", "9788983711892",
        bd(4.7), 160, date(2006, 12, 20), 140));
    return books;
  }


  public static Book save(EntityManager em, String title, String author, String isbn,
      BigDecimal rating, int reviewCount, LocalDate publishedDate, int createdAtMinutes) {

    Book book = Book.builder()
        .title(title)
        .author(author)
        .description(title)
        .publisher(author)
        .publishedDate(publishedDate)
        .thumbnailUrl("temp/url/" + isbn)
        .isbn(isbn)
        .build();

    em.persist(book);
    em.flush();

    UUID id = book.getId();

    em.createQuery(
            "update Book b set b.rating = :rating, b.reviewCount = :reviewCount, "
                + "b.createdAt = :createdAt where b.id = :id")
        .setParameter("rating", rating)
        .setParameter("reviewCount", reviewCount)
        .setParameter("createdAt", BASE.plus(createdAtMinutes, ChronoUnit.MINUTES))
        .setParameter("id", id)
        .executeUpdate();

    em.clear();
    return em.find(Book.class, id);
  }

  private static BigDecimal bd(double v) {
    return BigDecimal.valueOf(v);
  }

  private static LocalDate date(int y, int m, int d) {
    return LocalDate.of(y, m, d);
  }
}
