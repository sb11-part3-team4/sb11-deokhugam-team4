package com.part3_team4.deokhoogam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.OcrSpaceApiClient;
import com.part3_team4.deokhoogam.domain.book.service.OcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

  @Mock
  private OcrSpaceApiClient ocrSpaceApiClient;

  @InjectMocks
  private OcrService ocrService;

  @Test
  @DisplayName("OCR 텍스트에서 13자리 ISBN 숫자만 하이픈 없이 추출한다")
  void extractIsbn_Success() {
    // given
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "book.jpg",
        "image/jpeg",
        "image".getBytes());

    String noisyText = "안녕하세요 ISBN 978-89-6540-260-2\n가격 15,000원";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(noisyText);

    // when
    String extractedIsbn = ocrService.extractIsbnFromImage(file);

    assertThat(extractedIsbn).isEqualTo("9788965402602");
  }

  @Test
  @DisplayName("OCR 텍스트에 13자리 숫자가 존재하지만 978/979로 시작하지 않으면 예외가 발생한다")
  void extractIsbn_Invalid13Digit_NotFound() {
    // given
    MockMultipartFile file = new MockMultipartFile("file", "book.jpg", "image/jpeg",
        "image".getBytes());

    String invalid13DigitText = "바코드 번호: 1234567890123";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(invalid13DigitText);

    // when & then
    assertThatThrownBy(() -> ocrService.extractIsbnFromImage(file))
        .isInstanceOf(OcrProcessingException.class);
  }

  @Test
  @DisplayName("OCR 텍스트에서 10자리 ISBN을 추출한다")
  void extractIsbn10_Numeric_Success() {
    // given
    MockMultipartFile file = new MockMultipartFile("file", "old-book.jpg", "image/jpeg",
        "image".getBytes());
    String textWithIsbn10 = "2007년 이전 책\nISBN 89-7914-118-5\n가격 8,000원";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(textWithIsbn10);

    // when
    String extractedIsbn = ocrService.extractIsbnFromImage(file);

    // then
    assertThat(extractedIsbn).isEqualTo("8979141185");
  }

  @Test
  @DisplayName("OCR 텍스트에서 10자리 ISBN 중 마지막 자리가 X인 경우를 추출한다")
  void extractIsbn10_WithX_Success() {
    // given
    MockMultipartFile file = new MockMultipartFile("file", "old-book2.jpg", "image/jpeg",
        "image".getBytes());
    String textWithIsbn10X = "고전 서적\nISBN 89-1234-567-X\n";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(textWithIsbn10X);

    // when
    String extractedIsbn = ocrService.extractIsbnFromImage(file);

    // then
    assertThat(extractedIsbn).isEqualTo("891234567X");
  }

  @Test
  @DisplayName("OCR 텍스트에 10자리 패턴이 존재하지만 마지막 자리가 유효한 체크문자가 아니면 예외가 발생한다")
  void extractIsbn_Invalid10Digit_NotFound() {
    // given
    MockMultipartFile file = new MockMultipartFile("file", "book.jpg", "image/jpeg",
        "image".getBytes());

    String invalid10DigitText = "2007년 이전 책 바코드: 89-1234-567-Y";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(invalid10DigitText);

    // when & then
    assertThatThrownBy(() -> ocrService.extractIsbnFromImage(file))
        .isInstanceOf(OcrProcessingException.class);
  }

  @Test
  @DisplayName("텍스트가 유효한 10/13자리 ISBN 패턴이 아니면 예외가 발생한다")
  void extractIsbn_NumbersExistButNotIsbn_NotFound() {
    // given
    MockMultipartFile file = new MockMultipartFile("file", "book.jpg", "image/jpeg",
        "image".getBytes());

    String textWithRandomNumbers = "초판 1쇄 2026년 6월 4일\n가격 15,000원\n분류번호 12345";
    given(ocrSpaceApiClient.extractTextFromImage(file)).willReturn(textWithRandomNumbers);

    // when & then
    assertThatThrownBy(() -> ocrService.extractIsbnFromImage(file))
        .isInstanceOf(OcrProcessingException.class);
  }
}