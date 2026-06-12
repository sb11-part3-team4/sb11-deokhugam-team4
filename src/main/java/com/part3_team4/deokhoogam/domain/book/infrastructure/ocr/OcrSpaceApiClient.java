package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.dto.OcrSpaceDto;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class OcrSpaceApiClient {

  private final RestClient restClient;
  private final String apiKey;

  public OcrSpaceApiClient(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${ocr.space.api-key}") String apiKey,
      @Value("${ocr.space.connect-timeout:5000}") int connectTimeout,
      @Value("${ocr.space.read-timeout:15000}") int readTimeout) {

    RestTemplate restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofMillis(connectTimeout))
        .readTimeout(Duration.ofMillis(readTimeout))
        .build();

    this.restClient = RestClient.builder(restTemplate)
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
      log.warn("OCR Space API 응답 오류: status={}", e.getStatusCode());
      throw ExternalApiException.withCause(
          ErrorCode.EXTERNAL_API_ERROR,
          "HTTP Status: " + e.getStatusCode(), e
      );
    } catch (RestClientException e) {

      log.warn("OCR Space API 통신 오류");
      throw ExternalApiException.withCause(
          ErrorCode.EXTERNAL_API_TIMEOUT,
          "OCR 서버와 통신 중 오류가 발생했습니다.", e
      );
    }
  }

  private List<OcrSpaceDto.ParsedResult> getValidatedResults(OcrSpaceDto response) {
    List<OcrSpaceDto.ParsedResult> results = response != null ? response.parsedResults() : null;

    if (response == null || response.isErroredOnProcessing() || results == null
        || results.isEmpty()) {
      log.warn("OCR Space API 처리 실패 응답: errored={}",
          response != null && response.isErroredOnProcessing());
      throw OcrProcessingException.from(ErrorCode.OCR_PROCESSING_FAILED);
    }

    List<OcrSpaceDto.ParsedResult> validResults = results.stream()
        .filter(Objects::nonNull)
        .filter(r -> r.parsedText() != null && !r.parsedText().isBlank())
        .toList();

    if (validResults.isEmpty()) {
      throw OcrProcessingException.from(ErrorCode.OCR_TEXT_NOT_FOUND);
    }
    return validResults;
  }
}