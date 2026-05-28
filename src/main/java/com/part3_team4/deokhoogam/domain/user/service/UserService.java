package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequest;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequest;
import java.util.UUID;

public interface UserService {

  UUID createUser(UserCreateRequest request);

  UserResponse getUser(UUID userId);

  void updateUser(UUID userId, UserUpdateRequest request);

  void deleteUser(UUID userId);
}
