package com.part3_team4.deokhoogam.domain.book.entity;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidDirectionException;
import java.util.Arrays;

public enum Direction {
  ASC,
  DESC;

  @JsonCreator
  public static Direction from(String value) {
    return Arrays.stream(Direction.values())
        .filter(d -> d.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(()->InvalidDirectionException.withValue(value));
  }
}
