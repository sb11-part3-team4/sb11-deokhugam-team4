package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.exception.InvalidCredentialsException;
import com.part3_team4.deokhoogam.domain.user.exception.PasswordMismatchException;
import com.part3_team4.deokhoogam.domain.user.exception.UserAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.mapper.UserMapper;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.global.jwt.JwtProvider;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService{

  private final UserRepository userRepository;
  private final DeletedUserRepository deleteUserRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  public UserServiceImpl(UserRepository userRepository, DeletedUserRepository deleteUserRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
    this.userRepository = userRepository;
    this.deleteUserRepository = deleteUserRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtProvider = jwtProvider;
  }

  @Override
  @Transactional
  public UserDto createUser(UserCreateRequestDto request) {
    if (userRepository.existsByName(request.nickname())) {
      throw UserAlreadyExistsException.withName();
    }
    if (userRepository.existsByEmail(request.email())) {
      throw UserAlreadyExistsException.withEmail();
    }
    if (deleteUserRepository.existsByEmail(request.email())) {
      throw UserAlreadyExistsException.withEmail();
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    User user = new User(request.email(), request.nickname(), encodedPassword);

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
  public void updateUser(UUID userId, UserUpdateRequestDto request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (request.newName() != null && !user.getName().equals(request.newName())) {
      if (userRepository.existsByName(request.newName())) {
        throw UserAlreadyExistsException.withName();
      }
      user.updateName(request.newName());
    }

    if (request.newEmail() != null && !user.getEmail().equals(request.newEmail())) {
      if (userRepository.existsByEmail(request.newEmail()) || deleteUserRepository.existsByEmail(request.newEmail())) {
        throw UserAlreadyExistsException.withEmail();
      }
      user.updateEmail(request.newEmail());
    }
  }

  @Override
  @Transactional
  public void updatePassword(UUID userId, PasswordUpdateRequestDto request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new PasswordMismatchException();
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

    user.softDelete();
  }

  @Override
  @Transactional(readOnly = true)
  public String login(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException();
    }

    return jwtProvider.createAccessToken(user.getEmail());
  }
}
