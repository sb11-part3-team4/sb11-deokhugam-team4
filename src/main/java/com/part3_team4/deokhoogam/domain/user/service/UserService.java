package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDto createUser(UserCreateRequestDto request);

  UserResponse getUser(UUID userId);

  void updateUser(UUID userId, UserUpdateRequestDto request);

  void updatePassword(UUID userId, PasswordUpdateRequestDto request);

  void deleteUser(UUID userId);

  void hardDeleteUser(UUID userId);

  UserLoginResultDto login(String email, String password);
}
