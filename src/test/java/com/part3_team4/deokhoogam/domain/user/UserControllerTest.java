package com.part3_team4.deokhoogam.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.user.controller.UserController;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Test
  @DisplayName("회원가입 API 성공 - 201 반환")
  void createUser_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UserCreateRequestDto request = new UserCreateRequestDto(
        "test@deokhugam.com", "testUser", "password123!");
    UserDto responseDto = new UserDto(
        userId, "test@deokhugam.com", "testUser", null);

    given(userService.createUser(any(UserCreateRequestDto.class), isNull()))
        .willReturn(responseDto);

    mockMvc.perform(post("/api/users/signup")
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

    mockMvc.perform(post("/api/users/signup")
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
        .andExpect(jsonPath("$.name").value("testUser"));
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
  @DisplayName("회원 탈퇴 성공 - 204반환")
  void deleteUser_success() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNoContent());
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
}
