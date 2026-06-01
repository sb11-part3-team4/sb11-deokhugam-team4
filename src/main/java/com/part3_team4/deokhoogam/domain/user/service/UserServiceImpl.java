package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.exception.UserAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.mapper.UserMapper;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService{

  private final UserRepository userRepository;
  private final DeletedUserRepository deleteUserRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(UserRepository userRepository, DeletedUserRepository deleteUserRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.deleteUserRepository = deleteUserRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public UserDto createUser(UserCreateRequestDto request, MultipartFile profileImage) {
    if (userRepository.existsByName(request.name())) {
      throw UserAlreadyExistsException.withName();
    }
    if (userRepository.existsByEmail(request.email())) {
      throw UserAlreadyExistsException.withEmail();
    }
    if (deleteUserRepository.existsByEmail(request.email())) {
      throw UserAlreadyExistsException.withEmail();
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    User user = new User(request.email(), request.name(), encodedPassword);

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
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    return new UserResponse(user.getEmail(), user.getName());
  }

  @Override
  @Transactional
  public void updateUser(UUID userId, UserUpdateRequestDto request, MultipartFile profileImage) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (request.newName() != null && !user.getName().equals(request.newName())) {
      if (userRepository.existsByName(request.newName())) {
        throw UserAlreadyExistsException.withName();
      }
      user.updateName(request.newName());
    }

    if (request.newEmail() != null && !user.getEmail().equals(request.newEmail())) {
      if (userRepository.existsByEmail(request.newEmail())) {
        throw UserAlreadyExistsException.withEmail();
      }
      user.updateEmail(request.newEmail());
    }

    if (profileImage != null && !profileImage.isEmpty()) {
      //새로운 프로필 이미지 덮어쓰기 로직 추가 예정
    }
  }

  @Override
  @Transactional
  public void updatePassword(UUID userId, PasswordUpdateRequestDto request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
    }

    String encodedNewPassword = passwordEncoder.encode(request.newPassword());
    user.updatePassword(encodedNewPassword);
  }

  @Override
  @Transactional
  public void deleteUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    DeletedUser deletedUser = DeletedUser.from(user);

    deleteUserRepository.save(deletedUser);

    userRepository.delete(user);
  }
}
