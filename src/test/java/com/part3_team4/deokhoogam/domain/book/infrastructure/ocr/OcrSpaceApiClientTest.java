package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(
    value = OcrSpaceApiClient.class,
    properties = "ocr.space.api-key=test-ocr-key"
)
class OcrSpaceApiClientTest {

  @Autowired
  private OcrSpaceApiClient ocrSpaceApiClient;

  @Autowired
  private MockRestServiceServer mockServer;

  @Test
  @DisplayName("OCR 서버에서 처리 에러 응답 시 OcrProcessingException 발생")
  void extractText_ProcessingError() {
    // given
    MockMultipartFile mockFile = new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());

    String mockErrorResponse = """
        {
            "IsErroredOnProcessing": true,
            "ErrorMessage": ["Unable to recognize the file type"],
            "ParsedResults": []
        }
        """;

    mockServer.expect(requestTo("https://api.ocr.space/parse/image"))
        .andRespond(withSuccess(mockErrorResponse, MediaType.APPLICATION_JSON));

    // when & then
    assertThatThrownBy(() -> ocrSpaceApiClient.extractTextFromImage(mockFile))
        .isInstanceOf(OcrProcessingException.class);
  }

  @Test
  @DisplayName("OCR 서버 통신 중 500 서버 에러 발생 시 ExternalApiException 발생")
  void extractText_ServerError() {
    // given
    MockMultipartFile mockFile = new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());

    mockServer.expect(requestTo("https://api.ocr.space/parse/image"))
        .andRespond(withServerError());

    // when & then
    assertThatThrownBy(() -> ocrSpaceApiClient.extractTextFromImage(mockFile))
        .isInstanceOf(ExternalApiException.class);
  }
}