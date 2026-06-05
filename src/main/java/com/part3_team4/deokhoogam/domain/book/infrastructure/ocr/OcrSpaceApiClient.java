package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.dto.OcrSpaceDto;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrSpaceApiClient {

  private final RestClient restClient;
  private final String apiKey;

  public OcrSpaceApiClient(
      RestClient.Builder builder,
      @Value("${ocr.space.api-key}") String apiKey) {

    this.restClient = builder
        .baseUrl("https://api.ocr.space")
        .build();

    this.apiKey = apiKey;
  }

  public String extractTextFromImage(MultipartFile file) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());
    body.add("language", "eng");

    try {
      OcrSpaceDto response = restClient.post()
          .uri("/parse/image")
          .header("apikey", apiKey)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .body(OcrSpaceDto.class);

      List<OcrSpaceDto.ParsedResult> results = getValidatedResults(response);

      return results.stream()
          .map(OcrSpaceDto.ParsedResult::parsedText)
          .collect(Collectors.joining("\n"));

    } catch (RestClientResponseException e) {
      throw ExternalApiException.withCause(
          "OCR API 호출 실패. 상태 코드: " + e.getStatusCode(), e
      );
    } catch (RestClientException e) {
      throw ExternalApiException.withCause(
          "OCR 서버와 통신 중 오류가 발생했습니다.", e
      );
    }
  }

  private List<OcrSpaceDto.ParsedResult> getValidatedResults(OcrSpaceDto response) {
    List<OcrSpaceDto.ParsedResult> results =
        response != null ? response.parsedResults() : null;

    if (response == null
        || response.isErroredOnProcessing()
        || results == null
        || results.isEmpty()) {
      List<String> errorMessages = response != null ? response.errorMessage() : null;

      String detailMessage = errorMessages == null || errorMessages.isEmpty()
          ? "응답 데이터가 없습니다." : String.join(", ", errorMessages);

      throw OcrProcessingException.withDetail("OCR 이미지 처리 실패: " + detailMessage);
    }
    return results;
  }
}