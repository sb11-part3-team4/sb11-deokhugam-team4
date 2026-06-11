package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.OcrSpaceApiClient;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

  private final OcrSpaceApiClient ocrSpaceApiClient;

  private static final Pattern ISBN_PATTERN = Pattern.compile(
      "(?<!\\d)(97[89](?:[ -]*\\d){10}|(?:\\d[ -]*){9}[0-9Xx])(?![ -]*\\d)");

  public String extractIsbnFromImage(MultipartFile file) {
    validateFile(file);

    log.info("OCR ISBN 추출 요청: filename={}, size={}",
        file.getOriginalFilename(), file.getSize());

    String rawText = ocrSpaceApiClient.extractTextFromImage(file);

    String isbn = parseIsbnFromText(rawText);

    log.info("OCR ISBN 추출 성공: filename={}, isbn={}", file.getOriginalFilename(), isbn);
    return isbn;
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw OcrProcessingException.from(ErrorCode.OCR_EMPTY_FILE);
    }
  }

  private String parseIsbnFromText(String rawText) {
    Matcher matcher = ISBN_PATTERN.matcher(rawText);

    if (matcher.find()) {
      String extracted = matcher.group(1).replaceAll("[\\s-]", "");
      return extracted.toUpperCase();
    }
    log.warn("OCR 텍스트에서 ISBN 패턴 미발견");
    throw OcrProcessingException.from(ErrorCode.OCR_ISBN_NOT_FOUND);
  }
}