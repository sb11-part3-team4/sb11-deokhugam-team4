package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.part3_team4.deokhoogam.domain.user.dto.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequest;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequest;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.repository.DeleteUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.domain.user.service.UserServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private DeleteUserRepository deleteUserRepository;

  @Test
  @DisplayName("회원가입 성공")
  void signUp_success() {
    UserCreateRequest request = new UserCreateRequest(
        "test@deokhugam.com", "testUser", "password123!");

    given(userRepository.existsByName(request.name())).willReturn(false);
    given(userRepository.existsByEmail(request.email())).willReturn(false);

    UUID savedUserId = userService.createUser(request);

    then(userRepository).should().save(any(User.class));

    assertThat(savedUserId).isNotNull();
  }

  @Test
  @DisplayName("회원가입 실패 - 이미 존재하는 이름")
  void signUp_fail_duplicateName() {
    UserCreateRequest request = new UserCreateRequest(
        "duplicate@deokhugam.com", "testUser", "password123!");

    given(userRepository.existsByName(request.name())).willReturn(true);

    assertThrows(IllegalArgumentException.class, () -> {
      userService.createUser(request);
    });

    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
  void signUp_fail_duplicateEmail() {
    UserCreateRequest request = new UserCreateRequest(
        "duplicate@deokhugam.com", "testUser", "password123!");

    given(userRepository.existsByEmail(request.email())).willReturn(true);

    assertThrows(IllegalArgumentException.class, () -> {
      userService.createUser(request);
    });

    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원 정보 조회 성공")
  void getUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId,
        "test@deokhugam.com", "testUser", "password123!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    UserResponse response = userService.getUser(userId);

    assertThat(response.email()).isEqualTo("test@deokhugam.com");
    assertThat(response.name()).isEqualTo("testUser");
  }

  @Test
  @DisplayName("회원 정보 조회 실패 - 존재하지 않는 유저")
  void getUser_fail_userNotFound() {
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> {
      userService.getUser(userId);
    });
  }

  @Test
  @DisplayName("회원 정보 수정 성공")
  void updateUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId,
        "test@deokhugam.com", "testUser", "password123!");

    UserUpdateRequest request = new UserUpdateRequest(
        "newEmail@deokhugam.com","newName", "newPassword123!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    userService.updateUser(userId, request);

    assertThat(user.getName()).isEqualTo("newName");
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 이미 존재하는 이름")
  void updateUser_fail_duplicateName() {
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest(
        "newEmail@deokhugam.com","newName", "newPassword123!");

    given(userRepository.existsByName(request.newName())).willReturn(true);

    assertThrows(IllegalArgumentException.class, () -> {
      userService.updateUser(userId, request);
    });
  }

  @Test
  @DisplayName("회원 탈퇴 성공")
  void deleteUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId,
        "test@deokhugam.com", "testUser", "password123!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    userService.deleteUser(userId);

    then(userRepository).should().delete(user);
    then(deleteUserRepository).should().save(any(DeletedUser.class));
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 존재하지 않는 유저")
  void deleteUser_fail_userNotFound() {
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> {
      userService.deleteUser(userId);
    });

    then(deleteUserRepository).should(never()).save(any(DeletedUser.class));
    then(userRepository).should(never()).delete(any(User.class));
  }
}
