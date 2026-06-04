package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class NaverApiService {

  @Value("${naver.client.id}")
  private String clientId;

  @Value("${naver.client.secret}")
  private String clientSecret;

  public NaverBookDto getBookInfoByIsbn(String isbn) {
    //네이버 api
    String url = "https://openapi.naver.com/v1/search/book_adv.json";

    URI uri = UriComponentsBuilder.fromHttpUrl(url)
        .queryParam("d_isbn", isbn)
        .build()
        .toUri();

    // 키 달아주기
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Naver-Client-Id", clientId);
    headers.set("X-Naver-Client-Secret", clientSecret);

    HttpEntity<String> entity = new HttpEntity<>(headers);
    RestTemplate restTemplate = new RestTemplate();

    //get 요청
    try {
      ResponseEntity<NaverBookDto> response = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          entity,
          NaverBookDto.class
      );

      // 데이터 가져오기
      NaverBookDto body = response.getBody();

      // body check
      if (body != null) {
        return body;
      }

    } catch (Exception e) {
      log.warn("네이버 API 책 검색 중 예외가 발생했습니다. isbn : {}", isbn);
    }

    return null;
  }

}
