package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Test
  @DisplayName("OCR API 정상 응답 시 추출된 텍스트들을 줄바꿈으로 연결하여 반환한다")
  void extractText_Success() {
    // given
    MockMultipartFile mockFile = new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());

    String mockSuccessResponse = """
        {
            "IsErroredOnProcessing": false,
            "ParsedResults": [
                {"ParsedText": "978-89-1234567-8"},
                {"ParsedText": "도서명: 테스트"}
            ]
        }
        """;

    mockServer.expect(requestTo("https://api.ocr.space/parse/image"))
        .andRespond(withSuccess(mockSuccessResponse, MediaType.APPLICATION_JSON));

    // when
    String result = ocrSpaceApiClient.extractTextFromImage(mockFile);

    // then
    assertThat(result).isEqualTo("978-89-1234567-8\n도서명: 테스트");
  }

  @Test
  @DisplayName("OCR 서버 응답에 파싱된 결과 ParsedResults가 비어있으면 OcrProcessingException 발생")
  void extractText_EmptyResults() {
    // given
    MockMultipartFile mockFile = new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());

    String mockEmptyResponse = """
        {
            "IsErroredOnProcessing": false,
            "ParsedResults": []
        }
        """;

    mockServer.expect(requestTo("https://api.ocr.space/parse/image"))
        .andRespond(withSuccess(mockEmptyResponse, MediaType.APPLICATION_JSON));

    // when & then
    assertThatThrownBy(() -> ocrSpaceApiClient.extractTextFromImage(mockFile))
        .isInstanceOf(OcrProcessingException.class);
  }

  @Test
  @DisplayName("OCR 서버 응답에 ParsedResults 필드가 아예 없으면 OcrProcessingException 발생")
  void extractText_NullResults() {
    // given
    MockMultipartFile mockFile = new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());

    String mockNullResponse = """
        {
            "IsErroredOnProcessing": false
        }
        """;

    mockServer.expect(requestTo("https://api.ocr.space/parse/image"))
        .andRespond(withSuccess(mockNullResponse, MediaType.APPLICATION_JSON));

    // when & then
    assertThatThrownBy(() -> ocrSpaceApiClient.extractTextFromImage(mockFile))
        .isInstanceOf(OcrProcessingException.class);
  }
}