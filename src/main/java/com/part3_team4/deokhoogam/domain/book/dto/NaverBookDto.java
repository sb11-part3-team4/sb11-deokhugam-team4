package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

  @JsonProperty("pubdate")
  private String publishedDate;

  private String isbn;

  @JsonProperty("image")
  private String thumbnailImage;
}