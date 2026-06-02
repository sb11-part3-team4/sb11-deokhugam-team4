package com.part3_team4.deokhoogam.domain.user.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "\"user\"")
@Getter
public class User extends BaseEntity {

  @Column(unique = true, nullable = false)
  private String email;

  @Column(unique = true, name = "nickname", nullable = false)
  private String name;
  private String password;

//  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
//  @JoinColumn(name = "profile_image_id")
//  private UserProfileImage profileImage;

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

//  public void updateProfileImage(UserProfileImage profileImage) {
//    this.profileImage = profileImage;
//  }
}
