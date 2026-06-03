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

  @Column(name = "nickname")
  private String name;

  @Column(name = "deleted_at", nullable = false)
  private Instant deletedAt;

  public DeletedUser(UUID id, String email, String name) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.deletedAt = Instant.now();
  }

  public static DeletedUser from(User user) {
    return new DeletedUser(user.getId(), user.getEmail(), user.getName());
  }
}
