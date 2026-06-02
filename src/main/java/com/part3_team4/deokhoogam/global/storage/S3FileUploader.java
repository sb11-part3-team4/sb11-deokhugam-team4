package com.part3_team4.deokhoogam.global.storage;

import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileUploader implements FileUploader {

  private static final List<String> ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp");

  private final S3Client s3Client;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Override
  public String upload(MultipartFile file, String domainPath) {
    validateFile(file);
    validateDomainPath(domainPath);

    String key = createKey(domainPath, file.getOriginalFilename());

    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .contentType(file.getContentType())
          .build();

      s3Client.putObject(request,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      log.info("S3 파일 업로드 성공: {}", key);

      return generateFileUrl(key);

    } catch (IOException | S3Exception e) {
      log.error("S3 파일 업로드 실패: {}, 정리를 시작합니다.", key, e);

      cleanup(key);

      throw StorageOperationException.uploadFailed(key);
    }
  }


  private void validateFile(MultipartFile file) {
    if (file == null) {
      throw new IllegalArgumentException("파일이 존재하지 않습니다.");
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("파일 이름이 존재하지 않습니다.");
    }

    if (file.isEmpty()) {
      throw new IllegalArgumentException("비어있는 파일은 업로드할 수 없습니다.");
    }

    validateExtension(originalFilename);
  }

  private void validateDomainPath(String domainPath) {
    if (domainPath == null || domainPath.isBlank()) {
      throw new IllegalArgumentException("도메인 경로가 존재하지 않습니다.");
    }
  }

  private void validateExtension(String filename) {
    int lastDotIndex = filename.lastIndexOf(".");
    if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
      throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다.");
    }

    String extension = filename.substring(lastDotIndex + 1).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다.");
    }
  }

  private String createKey(String domainPath, String originalFilename) {
    return domainPath + "/" + UUID.randomUUID() + "_" + originalFilename;
  }

  private String generateFileUrl(String key) {
    return s3Client.utilities()
        .getUrl(builder -> builder.bucket(bucketName).key(key))
        .toString();
  }

  private void cleanup(String key) {
    try {
      s3Client.deleteObject(d -> d.bucket(bucketName).key(key));
      log.info("S3 파일 정리 완료: {}", key);
    } catch (Exception e) {
      log.error("S3 파일 정리 실패: {}", key, e);
    }
  }
}