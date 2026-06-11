package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.exception.ActiveUserHardDeleteException;
import com.part3_team4.deokhoogam.domain.user.exception.InvalidCredentialsException;
import com.part3_team4.deokhoogam.domain.user.exception.PasswordMismatchException;
import com.part3_team4.deokhoogam.domain.user.exception.UserAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.mapper.UserMapper;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.domain.user.service.UserServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private DeletedUserRepository deleteUserRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("회원가입 성공")
  void signUp_success() {
    UserCreateRequestDto request = new UserCreateRequestDto(
        "test@deokhugam.com", "testUser", "password123!");

    UserDto mockDto = new UserDto(UUID.randomUUID(), "test@deokhugam.com"
        , "testUser");

    given(userRepository.existsByName(request.nickname())).willReturn(false);
    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(deleteUserRepository.existsByEmail(request.email())).willReturn(false);
    given(passwordEncoder.encode(any(CharSequence.class))).willReturn("encodedPassword");
    given(userMapper.toDto(any(User.class))).willReturn(mockDto);

    UserDto savedUserDto = userService.createUser(request);

    then(userRepository).should().save(any(User.class));

    assertThat(savedUserDto.name()).isEqualTo("testUser");
  }

  @Test
  @DisplayName("회원가입 실패 - 이미 존재하는 이름")
  void signUp_fail_duplicateName() {
    UserCreateRequestDto request = new UserCreateRequestDto(
        "duplicate@deokhugam.com", "testUser", "password123!");

    given(userRepository.existsByName(request.nickname())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.createUser(request);
    });

    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
  void signUp_fail_duplicateEmail() {
    UserCreateRequestDto request = new UserCreateRequestDto(
        "duplicate@deokhugam.com", "testUser", "password123!");

    given(userRepository.existsByEmail(request.email())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.createUser(request);
    });

    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 탈퇴한 유저의 이메일과 중복")
  void signUp_fail_duplicateEmail_inDeletedUser() {
    UserCreateRequestDto request = new UserCreateRequestDto(
        "deleted@deokhugam.com", "testUser", "password123!");

    // 일반 유저 테이블엔 없지만, 탈퇴한 테이블에 존재한다고 가정
    given(userRepository.existsByName(request.nickname())).willReturn(false);
    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(deleteUserRepository.existsByEmail(request.email())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.createUser(request);
    });

    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원 정보 조회 성공")
  void getUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User(
        "test@deokhugam.com", "testUser", "password123!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    UserResponse response = userService.getUser(userId);

    assertThat(response.email()).isEqualTo("test@deokhugam.com");
    assertThat(response.nickname()).isEqualTo("testUser");
  }

  @Test
  @DisplayName("회원 정보 조회 실패 - 존재하지 않는 유저")
  void getUser_fail_userNotFound() {
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> {
      userService.getUser(userId);
    });
  }

  @Test
  @DisplayName("회원 정보 수정 성공")
  void updateUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User(
        "test@deokhugam.com", "testUser", "password123!");

    UserUpdateRequestDto request = new UserUpdateRequestDto(
        "email@deokhugam.com","nickname");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    userService.updateUser(userId, request);

    assertThat(user.getName()).isEqualTo("nickname");
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 이미 존재하는 이름")
  void updateUser_fail_duplicateName() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@deokhugam.com", "oldName", "password123!");

    UserUpdateRequestDto request = new UserUpdateRequestDto(
        "email@deokhugam.com","nickname");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByName(request.nickname())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.updateUser(userId, request);
    });
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 이미 존재하는 이메일")
  void updateUser_fail_duplicateEmail() {
    UUID userId = UUID.randomUUID();
    User user = new User(
        "old@deokhugam.com", "testUser", "password123!");
    UserUpdateRequestDto request = new UserUpdateRequestDto(
        "duplicate@deokhugam.com", "testUser");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByEmail(request.email())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.updateUser(userId, request);
    });
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 이미 존재하는 이메일 (탈퇴한 유저)")
  void updateUser_fail_duplicateEmail_inDeletedUser() {
    UUID userId = UUID.randomUUID();
    User user = new User("old@deokhugam.com", "testUser", "password123!");

    UserUpdateRequestDto request = new UserUpdateRequestDto(
        "deleted@deokhugam.com", "testUser");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByEmail(request.email())).willReturn(false);
    // 탈퇴한 테이블에 이메일이 존재한다고 가정
    given(deleteUserRepository.existsByEmail(request.email())).willReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> {
      userService.updateUser(userId, request);
    });
  }

  @Test
  @DisplayName("비밀번호 수정 성공")
  void updatePassword_success() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@deokhugam.com", "testUser", "encodedOldPassword");
    PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(
        "oldPassword", "newPassword");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(request.currentPassword(), user.getPassword())).willReturn(true);
    given(passwordEncoder.encode(request.newPassword())).willReturn("encodedNewPassword");

    userService.updatePassword(userId, request);

    assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
  }

  @Test
  @DisplayName("비밀번호 수정 실패 - 비밀번호 불일치")
  void updatePassword_fail_passwordMismatch() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@deokhugam.com", "testUser", "encodedOldPassword");
    PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(
        "oldPassword", "newPassword");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(request.currentPassword(), user.getPassword())).willReturn(false);

    assertThrows(PasswordMismatchException.class, () -> {
      userService.updatePassword(userId, request);
    });
  }

  @Test
  @DisplayName("회원 탈퇴 성공 - 백업 테이블 저장 및 본 테이블 물리 삭제")
  void deleteUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@deokhugam.com", "testUser", "password123!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    userService.deleteUser(userId);

    then(deleteUserRepository).should().save(any(DeletedUser.class));
    then(eventPublisher).should().publishEvent(new UserDeletedEvent(userId, false));
    then(userRepository).should().delete(user);
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 존재하지 않는 유저")
  void deleteUser_fail_userNotFound() {
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> {
      userService.deleteUser(userId);
    });

    then(deleteUserRepository).should(never()).save(any(DeletedUser.class));
    then(userRepository).should(never()).delete(any(User.class));
  }

  @Test
  @DisplayName("회원 물리 삭제 성공 - 삭제 및 이벤트 발생")
  void hardDeleteUser_success() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@deokhugam.com", "testUser", "password123!");
    DeletedUser deletedUser = DeletedUser.from(user);

    given(userRepository.existsById(userId)).willReturn(false);
    given(deleteUserRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    userService.hardDeleteUser(userId);

    then(deleteUserRepository).should().delete(deletedUser);
    then(eventPublisher).should().publishEvent(new UserDeletedEvent(userId, true));
  }

  @Test
  @DisplayName("회원 물리 삭제 실패 - 활동 중인 유저는 하드 삭제 불가 예외 발생")
  void hardDeleteUser_fail_activeUser() {
    UUID userId = UUID.randomUUID();

    given(userRepository.existsById(userId)).willReturn(true);

    assertThrows(ActiveUserHardDeleteException.class, () -> {
      userService.hardDeleteUser(userId);
    });

    then(deleteUserRepository).should(never()).findById(any());
    then(deleteUserRepository).should(never()).delete(any());
    then(eventPublisher).should(never()).publishEvent(any());
  }

  @Test
  @DisplayName("회원 물리 삭제 실패 - 양쪽 어디에도 존재하지 않는 유저는 예외 발생")
  void hardDeleteUser_fail_userNotFound() {
    UUID userId = UUID.randomUUID();

    given(userRepository.existsById(userId)).willReturn(false);
    given(deleteUserRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> {
      userService.hardDeleteUser(userId);
    });

    then(deleteUserRepository).should(never()).delete(any());
    then(eventPublisher).should(never()).publishEvent(any());
  }

  @Test
  @DisplayName("로그인 성공")
  void login_success() {
    String email = "test@deokhugam.com";
    String rawPassword = "password123!";
    User user = new User(email, "testUser", "encodedPassword");

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(rawPassword, user.getPassword())).willReturn(true);

    UserLoginResultDto result = userService.login(email, rawPassword);

    assertThat(result.nickname()).isEqualTo("testUser");
  }

  @Test
  @DisplayName("로그인 실패 - 존재하지 않는 이메일")
  void login_fail_emailNotFound() {
    String email = "notfound@deokhugam.com";

    given(userRepository.findByEmail(email)).willReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> {
      userService.login(email, "password123!");
    });
  }

  @Test
  @DisplayName("로그인 실패 - 비밀번호 불일치")
  void login_fail_passwordMismatch() {
    String email = "test@deokhugam.com";
    String rawPassword = "wrongPassword";
    User user = new User(email, "testUser", "encodedPassword");

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(rawPassword, user.getPassword())).willReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> {
      userService.login(email, rawPassword);
    });
  }

  @Test
  @DisplayName("로그인 실패 - 논리 삭제된 유저")
  void login_fail_deletedUser() {
    String email = "deleted@deokhugam.com";

    given(userRepository.findByEmail(email)).willReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> {
    userService.login(email, "password123!");
    });
  }

  @Test
  @DisplayName("다건 유저 닉네임 목록 조회(N+1 최적화용) 성공")
  void getUserNicknames_success() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    User user1 = mock(User.class);
    given(user1.getId()).willReturn(userId1);
    given(user1.getName()).willReturn("user1");

    User user2 = mock(User.class);
    given(user2.getId()).willReturn(userId2);
    given(user2.getName()).willReturn("user2");

    List<UUID> userIds = List.of(userId1, userId2);
    given(userRepository.findAllById(userIds)).willReturn(List.of(user1, user2));

    Map<UUID, String> nicknames = userService.getUserNicknames(userIds);

    assertThat(nicknames).hasSize(2);
    assertThat(nicknames.get(userId1)).isEqualTo("user1");
    assertThat(nicknames.get(userId2)).isEqualTo("user2");
  }
}
