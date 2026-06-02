package com.part3_team4.deokhoogam.global.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import com.part3_team4.deokhoogam.global.exception.storage.InvalidFileException;
import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileUploader implements FileUploader {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

  private final S3Template s3Template;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Override
  public String upload(MultipartFile file, String domainPath) {
    validateFile(file);
    validateDomainPath(domainPath);

    String key = createKey(domainPath, file.getOriginalFilename());

    try {
      S3Resource s3Resource = s3Template.upload(
          bucketName,
          key,
          file.getInputStream(),
          null
      );
      log.info("S3 파일 업로드 성공: {}", key);
      return s3Resource.getURL().toString();

    } catch (Exception e) {
      log.error("S3 파일 전송/인프라 장애 발생: {}", key, e);
      cleanup(key);

      throw StorageOperationException.uploadFailed(key, e);
    }
  }

  @Override
  public void delete(String fileUrl) {
    log.info("S3 파일 삭제 로직 실행: {}", fileUrl);
    // 실제 삭제 로직은 추후 구현 예정입니다.
  }

  private void validateFile(MultipartFile file) {
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
    return domainPath + "/" + UUID.randomUUID() + "_" + originalFilename;
  }

  private void cleanup(String key) {
    try {
      s3Template.deleteObject(bucketName, key);
      log.info("S3 파일 정리 완료: {}", key);
    } catch (Exception e) {
      log.error("S3 파일 정리 실패: {}", key, e);
    }
  }
}