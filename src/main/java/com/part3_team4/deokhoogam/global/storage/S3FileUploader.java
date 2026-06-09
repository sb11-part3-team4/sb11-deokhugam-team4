package com.part3_team4.deokhoogam.global.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import com.part3_team4.deokhoogam.global.exception.storage.InvalidFileException;
import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileUploader implements FileUploader {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
  private static final Set<String> ALLOWED_MIME = Set.of("image/png", "image/jpeg",
      "image/webp");

  private static final Tika TIKA = new Tika();

  private final S3Template s3Template;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Override
  public String upload(MultipartFile file, String domainPath) {
    String originalFilename = validateFile(file);
    validateDomainPath(domainPath);

    String key = createKey(domainPath, originalFilename);

    try {
      S3Resource s3Resource = s3Template.upload(
          bucketName,
          key,
          file.getInputStream(),
          null
      );
      log.info("S3 파일 업로드 성공: {}", key);
      return s3Resource.getURL().toString();

    } catch (S3Exception | IOException e) {
      log.error("S3 파일 전송/인프라 장애 발생: {}", key, e);
      cleanup(key);

      throw StorageOperationException.uploadFailed(key, e);
    }
  }

  @Override
  public void delete(String fileUrl) {
    log.info("S3 파일 삭제 로직 실행: {}", fileUrl);

    if (fileUrl == null || fileUrl.isBlank()) {
      log.warn("삭제할 파일 URL이 존재하지 않습니다.");
      return;
    }

    //URL에서 Key 추출
    String key = extractKey(fileUrl);

    // SoftDelete
    cleanup(key);
  }

  private String validateFile(MultipartFile file) {
    if (file == null) {
      throw InvalidFileException.withField(ErrorKey.FILE, "파일이 존재하지 않습니다.");
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw InvalidFileException.withField(ErrorKey.FILE, "파일 이름이 존재하지 않습니다.");
    }

    if (file.isEmpty()) {
      throw InvalidFileException.withField(ErrorKey.FILE, "비어있는 파일은 업로드할 수 없습니다.");
    }

    validateExtension(originalFilename);

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase())) {
      throw InvalidFileException.withFieldAndValue(ErrorKey.FILE, String.valueOf(contentType),
          "지원하지 않는 파일 타입(MIME)입니다.");
    }

    try (InputStream inputStream = file.getInputStream()) {
      String detectedMimeType = TIKA.detect(inputStream);

      if (!ALLOWED_MIME.contains(detectedMimeType.toLowerCase())) {
        log.warn("파일 변조 의심: 요청된 MIME={}, 실제 MIME={}", contentType, detectedMimeType);
        throw InvalidFileException.withFieldAndValue(
            ErrorKey.FILE, detectedMimeType, "실제 파일 타입과 불일치합니다.");
      }
    } catch (IOException e) {
      throw InvalidFileException.withField(ErrorKey.FILE, "파일 검증 중 오류가 발생했습니다.");
    }
    return originalFilename;
  }

  private void validateDomainPath(String domainPath) {
    if (domainPath == null || domainPath.isBlank()) {
      throw InvalidFileException.withField(ErrorKey.DOMAIN_PATH, "도메인 경로가 존재하지 않습니다.");
    }
  }

  private void validateExtension(String filename) {
    int lastDotIndex = filename.lastIndexOf(".");

    if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
      throw InvalidFileException.withFieldAndValue(
          ErrorKey.FILE, filename, "지원하지 않는 파일 확장자입니다."
      );
    }

    String extension = filename.substring(lastDotIndex + 1).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw InvalidFileException.withFieldAndValue(
          ErrorKey.FILE, filename, "지원하지 않는 파일 확장자입니다."
      );
    }
  }

  private String createKey(String domainPath, String originalFilename) {
    int lastDotIndex = originalFilename.lastIndexOf(".");
    String extension = originalFilename.substring(lastDotIndex + 1).toLowerCase();

    return domainPath + "/" + UUID.randomUUID() + "." + extension;
  }

  private String extractKey(String fileUrl) {
    try {
      java.net.URI uri = new java.net.URI(fileUrl);
      String path = uri.getPath();

      //앞의 /삭제
      if (path != null && path.startsWith("/")) {
        return path.substring(1);
      }
      return path;
    } catch (java.net.URISyntaxException e) {
      log.error("잘못된 파일 URL 형식입니다: {}", fileUrl);
      throw InvalidFileException.withFieldAndValue(ErrorKey.FILE, fileUrl, "유효하지 않은 파일 URL입니다.");
    }
  }

  private void cleanup(String key) {
    try {
      s3Template.deleteObject(bucketName, key);
      log.info("S3 파일 정리 완료: {}", key);
    } catch (S3Exception e) {
      log.error("S3 파일 정리 실패: {}", key, e);
    }
  }
}