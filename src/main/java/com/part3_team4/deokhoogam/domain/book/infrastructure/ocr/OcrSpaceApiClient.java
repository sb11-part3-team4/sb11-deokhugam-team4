package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.dto.OcrSpaceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrSpaceApiClient {

  private final RestClient restClient;
  private final String apiKey;

  public OcrSpaceApiClient(
      RestClient.Builder builder,
      @Value("${ocr.space.api-key}") String apiKey) {

    this.apiKey = apiKey;
    this.restClient = builder
        .baseUrl("https://api.ocr.space")
        .build();
  }

  public String extractTextFromImage(MultipartFile file) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());
    body.add("language", "kor");

    OcrSpaceDto response = restClient.post()
        .uri("/parse/image")
        .header("apikey", apiKey)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .body(OcrSpaceDto.class);

    if (response == null || response.isErroredOnProcessing() || response.getParsedResults()
        .isEmpty()) {
      throw new OcrProcessingException("OCR 이미지 처리 중 오류가 발생했습니다.");
    }
    
    return response.getParsedResults().get(0).getParsedText();
  }
}