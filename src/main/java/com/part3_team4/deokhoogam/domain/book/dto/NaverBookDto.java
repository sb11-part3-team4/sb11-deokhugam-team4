package com.part3_team4.deokhoogam.domain.book.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaverBookDto {

  private String title;
  private String author;
  private String description;
  private String publisher;
  private String publishedDate;  // ISO 8601 (YYYY-MM-DD)
  private String isbn;
  private String thumbnailImage; // Base64 인코딩된 이미지
}