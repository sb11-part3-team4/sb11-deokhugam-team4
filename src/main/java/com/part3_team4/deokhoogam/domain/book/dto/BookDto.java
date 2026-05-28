package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {

  private UUID id;
  private String title;
  private String author;
  private String description;
  private String publisher;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate publishedDate;

  private String isbn;
  private String thumbnailUrl;
  private int reviewCount;
  private BigDecimal rating;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Instant createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Instant updatedAt;

  public static BookDto from(Book book) {
    return BookDto.builder()
        .id(book.getId())
        .title(book.getTitle())
        .author(book.getAuthor())
        .description(book.getDescription())
        .publisher(book.getPublisher())
        .publishedDate(book.getPublishedDate())
        .isbn(book.getIsbn())
        .thumbnailUrl(book.getThumbnailUrl())
        .reviewCount(book.getReviewCount())
        .rating(book.getRating())
        .createdAt(book.getCreatedAt())
        .updatedAt(book.getUpdatedAt())
        .build();
  }
}