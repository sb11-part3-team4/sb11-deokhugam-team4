package com.part3_team4.deokhoogam.domain.book.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orphan_thumbnail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrphanThumbnail extends BaseEntity {

  @Column(nullable = false, length = 512)
  private String fileUrl;

  public OrphanThumbnail(String fileUrl) {
    this.fileUrl = fileUrl;
  }
}