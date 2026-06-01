package com.part3_team4.deokhoogam.domain.book.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum SortType {


  TITLE("title"),
  PUBLISHED_DATE("publishedDate"),
  RATING("rating"),
  REVIEW_COUNT("reviewCount");


  private final String value;


  SortType(String value) {
    this.value = value;
  }

  @JsonCreator
  public static SortType from(String value) {
    return Arrays.stream(SortType.values())
        .filter(t -> t.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 정렬 기준입니다."));
  }
}
