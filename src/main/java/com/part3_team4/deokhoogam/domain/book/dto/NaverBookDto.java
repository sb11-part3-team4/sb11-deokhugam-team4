package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.part3_team4.deokhoogam.global.util.DateDeserializer;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Jacksonized
public class NaverBookDto {

  private String title;
  private String author;
  private String description;
  private String publisher;

  @JsonAlias("pubdate")
  @JsonDeserialize(using = DateDeserializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate publishedDate;
  private String isbn;

  @JsonAlias("image")
  private String thumbnailImage;
}