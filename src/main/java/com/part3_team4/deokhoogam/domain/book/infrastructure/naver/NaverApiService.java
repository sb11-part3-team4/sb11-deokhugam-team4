package com.part3_team4.deokhoogam.domain.book.infrastructure.naver;

import com.part3_team4.deokhoogam.domain.book.dto.NaverApiResponse;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    log.info("네이버 API 책 검색 요청: isbn={}", isbn);

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
        log.info("네이버 API 책 검색 성공: isbn={}", isbn);
        return bookDto;
      }
      
      log.info("네이버 API 책 검색 결과 없음: isbn={}", isbn);
      return null;

    } catch (Exception e) {
      log.warn("네이버 API 책 검색 중 예외 발생. isbn : {}", isbn, e);
      throw ExternalApiException.withIsbn(isbn);
    }
  }

  private String convertImageToBase64(String imageUrl) {
    if (imageUrl == null || !imageUrl.startsWith("http")) {
      return imageUrl;
    }
    try {

      ResponseEntity<byte[]> response = restClient.get()
          .uri(imageUrl)
          .retrieve()
          .toEntity(byte[].class);

      // content type 검증
      MediaType contentType = response.getHeaders().getContentType();
      if (contentType == null || !contentType.getType().equals("image")) {
        log.warn("이미지가 아닌 컨텐츠 타입: {}", contentType);
        return null;
      }

      byte[] imageBytes = response.getBody();

      // 크기 제한
      if (imageBytes == null || imageBytes.length > 5 * 1024 * 1024) { //5mb
        log.warn("이미지 크기 초과: {} bytes", imageBytes == null ? 0 : imageBytes.length);
        return null;
      }

      return Base64.getEncoder().encodeToString(imageBytes);

    } catch (Exception e) {
      log.warn("이미지 변환 실패: {}", imageUrl, e);
      return null;
    }
  }
}