package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequest;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequest;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.repository.DeleteUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService{

  private final UserRepository userRepository;
  private final DeleteUserRepository deleteUserRepository;

  public UserServiceImpl(UserRepository userRepository, DeleteUserRepository deleteUserRepository) {
    this.userRepository = userRepository;
    this.deleteUserRepository = deleteUserRepository;
  }

  @Override
  public UUID createUser(UserCreateRequest request) {
    if (userRepository.existsByName(request.name())) {
      throw new IllegalArgumentException("이미 존재하는 이름입니다.");
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }

    User user = new User(UUID.randomUUID(), request.email(), request.name(), request.password());

    userRepository.save(user);

    return user.getId();
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
  public void updateUser(UUID userId, UserUpdateRequest request) {
    if (userRepository.existsByName(request.newName())) {
      throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

    user.updateName(request.newName());
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
