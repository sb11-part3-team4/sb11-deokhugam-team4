package com.part3_team4.deokhoogam.global.common;

import java.util.List;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class PageResponse<T> {

  private final List<T> content;
  private final String nextCursor;
  private final String afterCursor;
  private final int size;
  private final Long totalElements;
  private final boolean hasNext;

  public PageResponse(List<T> content, String nextCursor, String afterCursor, int size,
      Long totalElements, boolean hasNext) {
    this.content = content;
    this.nextCursor = nextCursor;
    this.afterCursor = afterCursor;
    this.size = size;
    this.totalElements = totalElements;
    this.hasNext = hasNext;
  }
}
