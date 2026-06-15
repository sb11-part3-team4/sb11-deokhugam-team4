package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import java.util.List;
import java.util.Map;
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

  // 💡 N+1 최적화를 위한 다건 닉네임 조회 메서드 추가
  Map<UUID, String> getUserNicknames(List<UUID> userIds);
}
