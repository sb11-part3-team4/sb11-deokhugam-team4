package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ExternalApiException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(
    value = OcrSpaceApiClient.class,
    properties = {
        "ocr.space.api-key=test-ocr-key",
        "ocr.space.connect-timeout=5000",
        "ocr.space.read-timeout=15000"
    }
)
class OcrSpaceApiClientTest {

  @Autowired
  private OcrSpaceApiClient ocrSpaceApiClient;

  @Autowired
  private MockRestServiceServer mockServer;

  private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

  @Test
  @DisplayName("OCR 서버에서 처리 에러 응답 시 OcrProcessingException 발생")
  void extractText_ProcessingError() {
    // given
    MockMultipartFile mockFile = createMockFile();
    String mockErrorResponse = """
        {
            "IsErroredOnProcessing": true,
            "ErrorMessage": ["Unable to recognize the file type"],
            "ParsedResults": []
        }
        """;

    mockServer.expect(requestTo(OCR_API_URL))
        .andRespond(withSuccess(mockErrorResponse, MediaType.APPLICATION_JSON));

    // when
    OcrProcessingException ex = assertThrows(OcrProcessingException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OCR_PROCESSING_FAILED);
  }

  @Test
  @DisplayName("OCR 서버 통신 중 500 서버 에러 발생 시 ExternalApiException 발생")
  void extractText_ServerError() {
    // given
    MockMultipartFile mockFile = createMockFile();
    mockServer.expect(requestTo(OCR_API_URL)).andRespond(withServerError());

    // when
    ExternalApiException ex = assertThrows(ExternalApiException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
  }

  @Test
  @DisplayName("OCR API 정상 응답 시 추출된 텍스트들을 줄바꿈으로 연결하여 반환한다")
  void extractText_Success() {
    // given
    MockMultipartFile mockFile = createMockFile();
    String mockSuccessResponse = """
        {
            "IsErroredOnProcessing": false,
            "ParsedResults": [
                {"ParsedText": "978-89-1234567-8"},
                {"ParsedText": "도서명: 테스트"}
            ]
        }
        """;

    mockServer.expect(requestTo(OCR_API_URL))
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
    MockMultipartFile mockFile = createMockFile();
    String mockEmptyResponse = """
        {
            "IsErroredOnProcessing": false,
            "ParsedResults": []
        }
        """;

    mockServer.expect(requestTo(OCR_API_URL))
        .andRespond(withSuccess(mockEmptyResponse, MediaType.APPLICATION_JSON));

    // when
    OcrProcessingException ex = assertThrows(OcrProcessingException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OCR_PROCESSING_FAILED);
  }

  @Test
  @DisplayName("OCR 서버 응답에 ParsedResults 필드가 아예 없으면 OcrProcessingException 발생")
  void extractText_NullResults() {
    // given
    MockMultipartFile mockFile = createMockFile();
    String mockNullResponse = """
        {
            "IsErroredOnProcessing": false
        }
        """;

    mockServer.expect(requestTo(OCR_API_URL))
        .andRespond(withSuccess(mockNullResponse, MediaType.APPLICATION_JSON));

    // when
    OcrProcessingException ex = assertThrows(OcrProcessingException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OCR_PROCESSING_FAILED);
  }

  @Test
  @DisplayName("OCR 응답은 성공이지만 parsedText가 모두 null이거나 공백이면 OcrProcessingException 발생")
  void extractText_BlankParsedText() {
    // given
    MockMultipartFile mockFile = createMockFile();
    String mockBlankTextResponse = """
        {
            "IsErroredOnProcessing": false,
            "ParsedResults": [
                {"ParsedText": null},
                {"ParsedText": "   "}
            ]
        }
        """;

    mockServer.expect(requestTo(OCR_API_URL))
        .andRespond(withSuccess(mockBlankTextResponse, MediaType.APPLICATION_JSON));

    // when
    OcrProcessingException ex = assertThrows(OcrProcessingException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OCR_TEXT_NOT_FOUND);
  }

  @Test
  @DisplayName("OCR 서버 통신 중 타임아웃 발생 시 ExternalApiException 예외로 변환된다")
  void extractText_TimeoutError() {
    // given
    MockMultipartFile mockFile = createMockFile();

    mockServer.expect(requestTo(OCR_API_URL))
        .andRespond(withException(new SocketTimeoutException("Read timed out")));

    // when
    ExternalApiException ex = assertThrows(ExternalApiException.class,
        () -> ocrSpaceApiClient.extractTextFromImage(mockFile));

    // then
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_TIMEOUT);
  }

  private MockMultipartFile createMockFile() {
    return new MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test-image-data".getBytes());
  }
}