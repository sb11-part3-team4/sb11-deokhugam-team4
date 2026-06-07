package com.part3_team4.deokhoogam.global.fixture;

import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import java.time.LocalDate;

public class NaverBookFixture {

  public static NaverBookDto createValidNaverBookDto(String isbn) {

    return NaverBookDto.builder()
        .title("모비 딕")
        .author("허먼 멜빌")
        .description("『모비 딕』 완역본")
        .isbn(isbn)
        .publisher("작가정신")
        .publishedDate(LocalDate.of(2024, 4, 9))
        .thumbnailImage("temp/url")
        .build();

  }
}
