package com.part3_team4.deokhoogam.global.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class S3FileUploader implements FileUploader {

  @Override
  public String upload(MultipartFile file, String domainPath) {
    return null;
  }
}