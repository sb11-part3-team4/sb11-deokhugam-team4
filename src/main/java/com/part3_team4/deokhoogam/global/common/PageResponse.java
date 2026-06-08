package com.part3_team4.deokhoogam.global.common;

import java.time.Instant;
import java.util.List;

/**
 * 커서 페이지네이션 응답 공통 DTO입니다.
 *
 * @param content 현재 페이지 데이터 목록
 * @param nextCursor 다음 페이지 조회에 사용할 커서
 * @param nextAfter 다음 페이지 조회에 사용할 보조 커서
 * @param size 현재 응답 데이터 개수
 * @param totalElements 전체 데이터 개수
 * @param hasNext 다음 페이지 존재 여부
 */
public record PageResponse<T> (

    List<T> content,
    String nextCursor,
    String nextAfter,
    int size,
    Long totalElements,
    boolean hasNext

) {
}