package com.part3_team4.deokhoogam.domain.book.dto;

import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Builder;

//도서 목록 요청
@Builder
public record BookGetListRequest(

    //검색 키워드
    @Size(max = 255, message = "키워드의 최대 길이는 255자입니다.")
    String keyword,

    //정렬 기준
    SortType orderBy,

    //정렬 방향
    Direction direction,

    //1차 커서
    @Size(max = 255, message = "커서의 최대 길이는 255자입니다.")
    String cursor,

    //2차 커서
    Instant after,

    //페이지 크기
    @Min(value = 1, message = "데이터 조회 개수는 최소 1개 이상이어야 합니다.")
    @Max(value = 100, message = "데이터 조회 개수는 한 번에 100개를 초과할 수 없습니다.")
    Integer limit

) {

  //컴팩트 생성자
  public BookGetListRequest {
    if (orderBy == null) {
      orderBy = SortType.TITLE;
    }
    if (direction == null) {
      direction = Direction.ASC;
    }
    if (limit == null) {
      limit = 50;
    }
  }
}
