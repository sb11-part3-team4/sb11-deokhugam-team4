package com.part3_team4.deokhoogam.domain.user.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "user_profile_image")
@Getter
public class UserProfileImage extends BaseEntity {

  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false)
  private Long size;

  @Column(nullable = false)
  private String contentType;

  protected UserProfileImage() {}

  public UserProfileImage(String fileName, Long size, String contentType) {
    this.fileName = fileName;
    this.size = size;
    this.contentType = contentType;
  }
}
