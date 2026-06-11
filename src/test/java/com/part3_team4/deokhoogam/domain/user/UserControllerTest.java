package com.part3_team4.deokhoogam.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.user.controller.UserController;
import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.exception.PasswordMismatchException;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private PowerUserRankingService powerUserRankingService;

  @Test
  @DisplayName("회원가입 API 성공 - 201 반환")
  void createUser_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UserCreateRequestDto request = new UserCreateRequestDto(
        "test@deokhugam.com", "testUser", "password123!");
    UserDto responseDto = new UserDto(
        userId, "test@deokhugam.com", "testUser");

    given(userService.createUser(any(UserCreateRequestDto.class)))
        .willReturn(responseDto);

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@deokhugam.com"))
        .andExpect(jsonPath("$.name").value("testUser"));
  }

  @Test
  @DisplayName("회원가입 실패 - 잘못된 형식의 이메일 400 반환")
  void createUser_fail_invalidEmail() throws Exception {
    UserCreateRequestDto request = new UserCreateRequestDto(
        "test-email", "testUser", "password123!");

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON-002"));
  }

  @Test
  @DisplayName("회원 정보 조회 성공 - 200 반환")
  void getUser_success() throws Exception {
    UUID userId = UUID.randomUUID();

    UserResponse responseDto = new UserResponse(
        "test@deokhugam.com", "testUser");

    given(userService.getUser(userId)).willReturn(responseDto);

    mockMvc.perform(get("/api/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@deokhugam.com"))
        .andExpect(jsonPath("$.nickname").value("testUser"));
  }

  @Test
  @DisplayName("회원 정보 조회 실패 - 존재하지 않는 유저 404 반환")
  void getUser_fail_userNotFound() throws Exception {
    UUID userId = UUID.randomUUID();

    given(userService.getUser(userId)).willThrow(
        UserNotFoundException.withId(userId));

    mockMvc.perform(get("/api/users/{userId}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER-001"));
  }

  @Test
  @DisplayName("회원 정보 수정 성공 - 200 반환")
  void updateUser_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UserUpdateRequestDto request = new UserUpdateRequestDto("newTest@deokhugam.com", "newTestUser");

    mockMvc.perform(patch("/api/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 잘못된 형식의 이메일 400 반환")
  void updateUser_fail_invalidEmail() throws Exception {
    UUID userId = UUID.randomUUID();
    UserUpdateRequestDto request = new UserUpdateRequestDto("invalid-email", "newTestUser");

    mockMvc.perform(patch("/api/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON-002"));
  }

  @Test
  @DisplayName("비밀번호 수정 성공 - 200반환")
  void updatePassword_success() throws Exception {
    UUID userId = UUID.randomUUID();
    PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(
        "oldPassword", "newPassword");

    mockMvc.perform(patch("/api/users/{userId}/password", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("비밀번호 수정 실패 - 비밀번호 불일치 400반환")
  void updatePassword_fail_passwordMismatch() throws Exception {
    UUID userId = UUID.randomUUID();
    PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(
        "oldPassword", "newPassword");

    willThrow(new PasswordMismatchException())
        .given(userService).updatePassword(userId, request);

    mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER-003"));
  }

  @Test
  @DisplayName("회원 탈퇴 성공 - 204반환")
  void deleteUser_success() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNoContent());

    then(userService).should().deleteUser(userId);
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 존재하지 않는 유저 404 반환")
  void deleteUser_fail_userNotFound() throws Exception {
    UUID userId = UUID.randomUUID();

    willThrow(UserNotFoundException.withId(userId))
        .given(userService).deleteUser(userId);

    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("회원 물리 삭제 성공 - 204 반환")
  void hardDeleteUser_success() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/users/{userId}/hard", userId))
        .andExpect(status().isNoContent());

    then(userService).should().hardDeleteUser(userId);
  }

  @Test
  @DisplayName("로그인 성공 - 200반환")
  void login_success() throws Exception {
    UserLoginRequestDto request = new UserLoginRequestDto(
        "test@deokhugam.com", "password123!");

    UserLoginResultDto mockResult = new UserLoginResultDto(
        UUID.randomUUID(), "test@deokhugam.com", "testUser", java.time.Instant.now());

    given(userService.login(request.email(), request.password()))
        .willReturn(mockResult);

    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@deokhugam.com"));
  }

  @Test
  @DisplayName("로그인 실패 - 이메일 형식 오류  400 반환")
  void login_fail_invalidEmail() throws Exception {
    UserLoginRequestDto request = new UserLoginRequestDto(
        "invalid-email-format", "password123!");

    mockMvc.perform(post("/api/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON-002"));
  }

  @Test
  @DisplayName("파워 유저 조회 성공 - 200 반환")
  void getPowerUsers_success() throws Exception {
    given(powerUserRankingService.getRankingWithNickname(any(PowerUserPeriod.class))).willReturn(List.of());

    mockMvc.perform(get("/api/users/power?period=DAILY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty());
  }
}
