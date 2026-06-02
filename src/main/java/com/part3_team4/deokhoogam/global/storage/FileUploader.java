package com.part3_team4.deokhoogam.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

  /**
   * 파일을 검증 및 난수화하여 지정된 도메인 경로에 업로드하고, 접근 가능한 public URL을 반환합니다.
   *
   * @param file       업로드할 멀티파트 파일
   * @param domainPath 저장될 도메인 폴더 경로
   * @return 저장된 객체 URL 경로
   */
  String upload(MultipartFile file, String domainPath);

  void delete(String fileUrl);
}