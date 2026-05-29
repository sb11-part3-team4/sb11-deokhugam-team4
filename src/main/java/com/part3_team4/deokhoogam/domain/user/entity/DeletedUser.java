package com.part3_team4.deokhoogam.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "deleted_user")
@Getter
public class DeletedUser {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String name;

  protected DeletedUser() {
  }

  public DeletedUser(UUID id, String email, String name) {
    this.id = id;
    this.email = email;
    this.name = name;
  }

  public static DeletedUser from(User user) {
    return new DeletedUser(user.getId(), user.getEmail(), user.getName());
  }
}
