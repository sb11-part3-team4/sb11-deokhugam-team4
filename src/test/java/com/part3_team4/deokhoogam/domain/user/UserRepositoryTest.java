package com.part3_team4.deokhoogam.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
public class UserRepositoryTest {

  @TestConfiguration
  @EnableJpaAuditing
  static class JpaAuditingConfig{}

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("유저 저장 성공")
  void saveUser_success() {
    User user = new User(
        "test@deokhugam.com", "testUser", "password123!");

    User saveUser = userRepository.save(user);

    assertThat(saveUser).isNotNull();
    assertThat(saveUser.getEmail()).isEqualTo("test@deokhugam.com");
    assertThat(saveUser.getName()).isEqualTo("testUser");
  }

  @Test
  @DisplayName("유저 저장 실패 - 이름 중복")
  void saveUser_fail_duplicateName() {
    User user1 = new User(
        "duplicate1@deokhugam.com", "user", "password123!");
    userRepository.save(user1);

    User user2 = new User(
        "duplicate2@deokhugam.com", "user", "password456!");
    assertThrows(DataIntegrityViolationException.class, () ->
        userRepository.saveAndFlush(user2));
  }

  @Test
  @DisplayName("유저 저장 실패 - 이메일 중복")
  void saveUser_fail_duplicateEmail() {
    User user1 = new User(
        "duplicate@deokhugam.com", "user1", "password123!");
    userRepository.save(user1);

    User user2 = new User(
        "duplicate@deokhugam.com", "user2", "password456!");
    assertThrows(DataIntegrityViolationException.class, () ->
      userRepository.saveAndFlush(user2));
  }

  @Test
  @DisplayName("유저 조회 성공")
  void findUser_success() {
    User user = new User(
        "find@deokhugam.com", "finder", "password123!");
    User saveUser = userRepository.save(user);

    Optional<User> foundUser = userRepository.findById(saveUser.getId());

    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo("find@deokhugam.com");
  }

  @Test
  @DisplayName("유저 조회 실패 - 존재하지 않는 유저")
  void findUser_fail_notFound() {
    UUID userId = UUID.randomUUID();

    Optional<User> foundUser = userRepository.findById(userId);

    assertThat(foundUser).isEmpty();
  }

  @Test
  @DisplayName("유저 삭제 성공")
  void deleteUser_success() {
    User user = new User(
        "test@deokhugam.com", "testUser", "password123!");
    User saveUser = userRepository.save(user);

    userRepository.delete(saveUser);

    Optional<User> foundUser = userRepository.findById(saveUser.getId());

    assertThat(foundUser).isEmpty();
  }

  @Test
  @DisplayName("이름 중복 검사 성공 - 존재하는 이름 true 반환")
  void existsByName_success_returnTrue() {
    User user = new User(
        "test@deokhugam.com", "testUser", "password123!");
    userRepository.save(user);

    boolean exists = userRepository.existsByName("testUser");

    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("이름 중복 검사 성공 - 존재하지 않는 이름 false 반환")
  void existsByNamee_success_returnFalse() {
    boolean exists = userRepository.existsByName("uniqueName");

    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("이메일 중복 검사 - 존재하는 이메일 true 반환")
  void existsByEmail_success_returnTrue() {
    User user = new User(
        "duplicate@deokhugam.com", "testUser", "password123!");
    userRepository.save(user);

    boolean exists = userRepository.existsByEmail("duplicate@deokhugam.com");

    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("이메일 중복 검사 - 조재하지 않는 이메일 false 반환")
  void existsByEmail_success_returnFalse() {
    boolean exists = userRepository.existsByEmail("unique@deokhugam.com");

    assertThat(exists).isFalse();
  }
}
