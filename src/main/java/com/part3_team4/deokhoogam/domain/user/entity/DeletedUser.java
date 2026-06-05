package com.part3_team4.deokhoogam.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deleted_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletedUser {

  @Id
  private UUID id;

  private String email;

  private String password;

  @Column(name = "nickname")
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at", nullable = false)
  private Instant deletedAt;

  public DeletedUser(UUID id, String email, String name, String password, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.password = password;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = Instant.now();
  }

  public static DeletedUser from(User user) {
    return new DeletedUser(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getPassword(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }
}
