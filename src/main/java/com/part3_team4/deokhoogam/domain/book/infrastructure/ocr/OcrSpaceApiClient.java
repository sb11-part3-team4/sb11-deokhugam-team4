package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrSpaceApiClient {

  private final RestClient restClient;

  public OcrSpaceApiClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  // 텍스트 추출
  public String extractTextFromImage(MultipartFile file) {
    return null;
  }
}