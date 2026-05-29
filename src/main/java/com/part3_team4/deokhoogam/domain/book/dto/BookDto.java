package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record BookDto(
    UUID id,
    String title,
    String author,
    String description,
    String publisher,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate publishedDate,

    String isbn,
    String thumbnailUrl,
    int reviewCount,
    BigDecimal rating,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    Instant createdAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    Instant updatedAt
) {

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