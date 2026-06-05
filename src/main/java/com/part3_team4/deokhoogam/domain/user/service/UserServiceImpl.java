package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.exception.InvalidCredentialsException;
import com.part3_team4.deokhoogam.domain.user.exception.PasswordMismatchException;
import com.part3_team4.deokhoogam.domain.user.exception.UserAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.mapper.UserMapper;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService{

  private final UserRepository userRepository;
  private final DeletedUserRepository deleteUserRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;

  public UserServiceImpl(UserRepository userRepository, DeletedUserRepository deleteUserRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.deleteUserRepository = deleteUserRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.eventPublisher = eventPublisher;
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

    if (request.nickname() != null && !user.getName().equals(request.nickname())) {
      if (userRepository.existsByName(request.nickname())) {
        throw UserAlreadyExistsException.withName();
      }
      user.updateName(request.nickname());
    }

    if (request.email() != null && !user.getEmail().equals(request.email())) {
      if (userRepository.existsByEmail(request.email()) || deleteUserRepository.existsByEmail(request.email())) {
        throw UserAlreadyExistsException.withEmail();
      }
      user.updateEmail(request.email());
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

    eventPublisher.publishEvent(new UserDeletedEvent(userId));

    userRepository.delete(user);
  }

  @Override
  @Transactional
  public void hardDeleteUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    eventPublisher.publishEvent(new UserDeletedEvent(userId));
    userRepository.delete(user);

    deleteUserRepository.findById(userId).ifPresent(deletedUser -> {
      deleteUserRepository.delete(deletedUser);
    });
  }

  @Override
  @Transactional(readOnly = true)
  public UserLoginResultDto login(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException();
    }

    return new UserLoginResultDto(
        "", user.getId(), user.getEmail(), user.getName(), user.getCreatedAt()
    );
  }
}
