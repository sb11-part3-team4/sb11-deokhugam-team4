package com.part3_team4.deokhoogam.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public class BaseEntity {

  @Id
  private UUID id;
  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at")
  @LastModifiedDate
  private Instant updatedAt;

  public BaseEntity() {
    this.id = UUID.randomUUID();
  }

  protected BaseEntity(UUID id, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
