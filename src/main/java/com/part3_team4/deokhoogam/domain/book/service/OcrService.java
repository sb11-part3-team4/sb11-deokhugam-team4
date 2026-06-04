package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.OcrSpaceApiClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrService {

  private final OcrSpaceApiClient ocrSpaceApiClient;

  private static final Pattern ISBN_PATTERN = Pattern.compile(
      "(?<!\\d)(97[89]\\d{10}|\\d{9}[0-9Xx])(?!\\d)");

  public String extractIsbnFromImage(MultipartFile file) {
    // 텍스트 추출
    String rawText = ocrSpaceApiClient.extractTextFromImage(file);

    // 공백 및 하이픈 제거
    String cleanText = rawText.replaceAll("[\\s-]", "");

    // 정규식 스캔
    Matcher matcher = ISBN_PATTERN.matcher(cleanText);

    if (matcher.find()) {
      // X로 끝나는 10자리 ISBN 검증을 위한 대문자 변환
      return matcher.group(1).toUpperCase();
    }

    throw OcrProcessingException.withDetail("이미지에서 ISBN 패턴을 찾을 수 없습니다.");
  }
}