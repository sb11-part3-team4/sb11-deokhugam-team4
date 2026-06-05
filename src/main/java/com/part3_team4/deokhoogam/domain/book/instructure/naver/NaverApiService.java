package com.part3_team4.deokhoogam.domain.book.instructure.naver;

import com.part3_team4.deokhoogam.domain.book.dto.NaverApiResponse;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import java.util.Base64;
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
      //Dto로 가져와서
      NaverApiResponse response = restClient.get()
          .uri(url, isbn)
          .header("X-Naver-Client-Id", clientId)
          .header("X-Naver-Client-Secret", clientSecret)
          .retrieve()
          .body(NaverApiResponse.class);

      // 안의 아이템(NaverBookDto) 만 가져오기
      if (response != null && response.items() != null && !response.items().isEmpty()) {
        NaverBookDto bookDto = response.items().get(0);
        bookDto.setThumbnailImage(convertImageToBase64(bookDto.getThumbnailImage()));
        return bookDto;
      }

      return null;

    } catch (Exception e) {
      log.warn("네이버 API 책 검색 중 예외 발생. isbn : {}", isbn, e);
      throw ExternalApiException.withIsbn(isbn);
    }
  }

  private String convertImageToBase64(String imageUrl) {
    if (imageUrl == null || imageUrl.startsWith("data:")) {
      return imageUrl;
    }
    try {
      byte[] imageBytes = restClient.get()
          .uri(imageUrl)
          .retrieve()
          .body(byte[].class);
      return Base64.getEncoder().encodeToString(imageBytes);
    } catch (Exception e) {
      return null;
    }
  }
}