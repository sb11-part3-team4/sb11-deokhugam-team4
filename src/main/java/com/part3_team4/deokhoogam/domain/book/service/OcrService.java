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
      "(?<!\\d)(97[89](?:[\\s-]*\\d){10}|(?:\\d[\\s-]*){9}[0-9Xx])(?!\\d)");

  public String extractIsbnFromImage(MultipartFile file) {
    String rawText = ocrSpaceApiClient.extractTextFromImage(file);

    return parseIsbnFromText(rawText);
  }

  private String parseIsbnFromText(String rawText) {
    Matcher matcher = ISBN_PATTERN.matcher(rawText);

    if (matcher.find()) {
      String extracted = matcher.group(1).replaceAll("[\\s-]", "");
      return extracted.toUpperCase();
    }

    throw OcrProcessingException.withDetail("이미지에서 ISBN 패턴을 찾을 수 없습니다.");
  }
}