package com.part3_team4.deokhoogam.domain.book.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidSortTypeException;
import java.util.Arrays;


public enum SortType {


  TITLE("title"),
  PUBLISHED_DATE("publishedDate"),
  RATING("rating"),
  REVIEW_COUNT("reviewCount");


  private final String value;


  SortType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static SortType from(String value) {
    return Arrays.stream(SortType.values())
        .filter(t -> t.value.equals(value))
        .findFirst()
        .orElseThrow(() -> InvalidSortTypeException.withValue(value));
  }
}
