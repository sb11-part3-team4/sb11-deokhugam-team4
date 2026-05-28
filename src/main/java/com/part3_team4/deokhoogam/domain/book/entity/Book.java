package com.part3_team4.deokhoogam.domain.book.entity;

import com.part3_team4.deokhoogam.domain.book.exception.InvalidBookException;
import com.part3_team4.deokhoogam.global.common.BaseEntity;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Book extends BaseEntity {

  private static final BigDecimal MIN_RATING = new BigDecimal("0.00");
  private static final BigDecimal MAX_RATING = new BigDecimal("5.00");

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "author", nullable = false, length = 100)
  private String author;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "publisher", nullable = false, length = 100)
  private String publisher;

  @Column(name = "published_date", nullable = false)
  private LocalDate publishedDate;

  @Column(name = "isbn", unique = true, length = 20)
  private String isbn;

  @Column(name = "thumbnail_url", length = 512)
  private String thumbnailUrl;

  @Column(name = "review_count", nullable = false)
  private int reviewCount;

  @Column(name = "rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal rating;

  @Builder
  public Book(String title, String author, String description, String publisher,
      LocalDate publishedDate, String isbn, String thumbnailUrl) {
    validateBookInfo(title, author, description, publisher, publishedDate);
    validateIsbn(isbn);

    this.title = title;
    this.author = author;
    this.description = description;
    this.publisher = publisher;
    this.publishedDate = publishedDate;
    this.isbn = isbn;
    this.thumbnailUrl = thumbnailUrl;

    this.reviewCount = 0;
    this.rating = new BigDecimal("0.00");
  }

  // ===== 수정 메서드 =====

  public void updateBookInfo(String title, String author, String description,
      String publisher, LocalDate publishedDate, String thumbnailUrl) {
    validateBookInfo(title, author, description, publisher, publishedDate);

    this.title = title;
    this.author = author;
    this.description = description;
    this.publisher = publisher;
    this.publishedDate = publishedDate;
    this.thumbnailUrl = thumbnailUrl;
  }

  public void updateReviewData(int newReviewCount, BigDecimal newRating) {
    validateReviewData(newReviewCount, newRating);

    this.reviewCount = newReviewCount;
    this.rating = newRating;
  }

  // ===== 검증 메서드 =====
  private void validateBookInfo(
      String title,
      String author,
      String description,
      String publisher,
      LocalDate publishedDate
  ) {
    if (title == null || title.isBlank()) {
      throw InvalidBookException.withField(ErrorKey.BOOK_TITLE, "제목은 필수입니다.");
    }
    if (author == null || author.isBlank()) {
      throw InvalidBookException.withField(ErrorKey.BOOK_AUTHOR, "저자는 필수입니다.");
    }
    if (description == null || description.isBlank()) {
      throw InvalidBookException.withField(ErrorKey.BOOK_DESCRIPTION, "설명은 필수입니다.");
    }
    if (publisher == null || publisher.isBlank()) {
      throw InvalidBookException.withField(ErrorKey.BOOK_PUBLISHER, "출판사는 필수입니다.");
    }
    if (publishedDate == null) {
      throw InvalidBookException.withField(ErrorKey.BOOK_PUBLISHED_DATE, "출판일은 필수입니다.");
    }
  }

  private void validateIsbn(String isbn) {
    if (isbn != null && isbn.isBlank()) {
      throw InvalidBookException.withFieldAndValue(
          ErrorKey.BOOK_ISBN, isbn, "ISBN이 입력되면 공백이 될 수 없습니다.");
    }
  }

  private void validateReviewData(int reviewCount, BigDecimal rating) {
    if (reviewCount < 0) {
      throw InvalidBookException.withFieldAndValue(
          ErrorKey.BOOK_REVIEW_COUNT, reviewCount, "리뷰 수는 음수가 될 수 없습니다.");
    }

    if (rating == null) {
      throw InvalidBookException.withField(
          ErrorKey.BOOK_RATING, "평점은 필수입니다.");
    }
    if (rating.compareTo(MIN_RATING) < 0 || rating.compareTo(MAX_RATING) > 0) {
      throw InvalidBookException.withFieldAndValue(
          ErrorKey.BOOK_RATING, rating, "평점은 0.00 ~ 5.00 사이여야 합니다."
      );
    }
  }
}