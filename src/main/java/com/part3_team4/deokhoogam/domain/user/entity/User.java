package com.part3_team4.deokhoogam.domain.user.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "\"user\"")
@Getter
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

  @Column(unique = true, nullable = false)
  private String email;

  @Column(unique = true, name = "nickname", nullable = false)
  private String name;
  private String password;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public User() {
  }

  public User(String email, String name, String password) {
    this.email = email;
    this.name = name;
    this.password = password;
  }

  public void updateName(String name) {
    this.name = name;
  }

  public void updateEmail(String email) {
    this.email = email;
  }

  public void updatePassword(String password) {
    this.password = password;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }
}
