package com.part3_team4.deokhoogam.domain.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delete_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeleteUser {

  @Id
  private UUID id;

  private String email;
  private String name;

  public DeleteUser(UUID id, String email, String name) {
    this.id = id;
    this.email = email;
    this.name = name;
  }

  public static DeleteUser from(User user) {
    return new DeleteUser(user.getId(), user.getEmail(), user.getName());
  }
}
