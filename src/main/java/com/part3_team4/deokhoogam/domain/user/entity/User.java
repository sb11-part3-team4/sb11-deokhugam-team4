package com.part3_team4.deokhoogam.domain.user.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "\"user\"")
@Getter
public class User extends BaseEntity {
  private String email;
  private String name;
  private String password;
  private String profileImageUrl;

  public User() {
  }

  public User(UUID uuid, String email, String name, String password) {
    this.email = email;
    this.name = name;
    this.password = password;
  }

  public void updateName(String name) {
    this.name = name;
  }
}
