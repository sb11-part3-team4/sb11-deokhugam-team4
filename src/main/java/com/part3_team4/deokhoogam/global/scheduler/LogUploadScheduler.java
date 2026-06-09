package com.part3_team4.deokhoogam.global.scheduler;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogUploadScheduler {

  private final S3Client s3Client;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Scheduled(cron = "0 0 0 * * *")
  public void uploadYesterdayLog() {

    // 전날 날짜로 파일명 생성
    String yesterday = LocalDate.now().minusDays(1)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String fileName = "deokhugam." + yesterday + ".log";
    File logFile = new File("logs/" + fileName);

    log.info("로그 파일 S3 업로드 시작: {}", fileName);

    if (!logFile.exists()) {
      log.warn("로그 파일이 존재하지 않습니다: {}", logFile.getPath());
      return;
    }

    try {
      // S3에 업로드
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucketName)
          .key("logs/" + fileName)
          .build();

      s3Client.putObject(request, logFile.toPath());
      log.info("로그 파일 S3 업로드 완료: {}", fileName);

      // 로컬 파일 삭제
      if (logFile.delete()) {
        log.info("로컬 로그 파일 삭제 완료: {}", fileName);
      } else {
        log.warn("로컬 로그 파일 삭제 실패: {}", fileName);
      }

    } catch (Exception e) {
      log.error("로그 파일 S3 업로드 실패: {}", fileName, e);
    }
  }
}