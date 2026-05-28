package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.mapper.UserMapper;
import com.part3_team4.deokhoogam.domain.user.repository.DeleteUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService{

  private final UserRepository userRepository;
  private final DeleteUserRepository deleteUserRepository;
  private final UserMapper userMapper;

  public UserServiceImpl(UserRepository userRepository, DeleteUserRepository deleteUserRepository, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.deleteUserRepository = deleteUserRepository;
    this.userMapper = userMapper;
  }

  @Override
  @Transactional
  public UserDto createUser(UserCreateRequestDto request, MultipartFile profileImage) {
    if (userRepository.existsByName(request.name())) {
      throw new IllegalArgumentException("이미 존재하는 이름입니다.");
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }

    User user = new User(request.email(), request.name(), request.password());

    if (profileImage != null && !profileImage.isEmpty()) {
      //프로필 이미지 저장 로직 추가 예정
    }

    userRepository.save(user);

    return userMapper.toDto(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
    return new UserResponse(user.getEmail(), user.getName());
  }

  @Override
  @Transactional
  public void updateUser(UUID userId, UserUpdateRequestDto request, MultipartFile profileImage) {
    if (userRepository.existsByName(request.newName())) {
      throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

    if (request.newName() != null) {
      user.updateName(request.newName());
    }

    if (profileImage != null && profileImage.isEmpty()) {
      //새로운 프로필 이미지 덮어쓰기 로직 추가 예정
    }
  }

  @Override
  public void deleteUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

    DeletedUser deletedUser = DeletedUser.from(user);

    deleteUserRepository.save(deletedUser);

    userRepository.delete(user);
  }
}
