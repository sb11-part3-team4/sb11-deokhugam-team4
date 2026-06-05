package com.part3_team4.deokhoogam.domain.book.instrcture.naver;

import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class NaverApiService {

  private final RestClient restClient;

  @Value("${naver.client.id}")
  private String clientId;

  @Value("${naver.client.secret}")
  private String clientSecret;

  // RestClient 설정 (타임아웃 적용)
  public NaverApiService(RestClient.Builder restClientBuilder) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000); // 연결 최대 3초
    factory.setReadTimeout(5000);    // 응답 대기 최대 5초

    this.restClient = restClientBuilder
        .requestFactory(factory)
        .build();
  }

  public NaverBookDto getBookInfoByIsbn(String isbn) {
    String url = "https://openapi.naver.com/v1/search/book_adv.json?d_isbn={isbn}";

    try {
      // 템플릿 기반으로 체이닝 호출

// record 버전은 get 없이 필드명 그대로 호출

      return restClient.get()
          .uri(url, isbn)
          .header("X-Naver-Client-Id", clientId)
          .header("X-Naver-Client-Secret", clientSecret)
          .retrieve()
          .body(NaverBookDto.class);

    } catch (Exception e) {
      log.warn("네이버 API 책 검색 중 예외 발생. isbn : {}", isbn, e);
      throw ExternalApiException.withIsbn(isbn);
    }
  }
}