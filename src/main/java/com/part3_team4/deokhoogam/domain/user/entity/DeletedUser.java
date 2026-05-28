package com.part3_team4.deokhoogam.domain.user.entity;

import java.util.UUID;

public class DeletedUser {
  private UUID id;
  private String email;
  private String name;

  public DeletedUser(UUID id, String email, String name) {
    this.id = id;
    this.email = email;
    this.name = name;
  }

  public static DeletedUser from(User user) {
    return new DeletedUser(user.getId(), user.getEmail(), user.getName());
  }
}
