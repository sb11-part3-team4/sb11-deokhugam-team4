package com.part3_team4.deokhoogam.domain.user.controller;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserDto> signup(
      @Valid @RequestBody UserCreateRequestDto request) {

    UserDto responseDto = userService.createUser(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserResponse> getUser(
      @PathVariable UUID userId) {

    UserResponse response = userService.getUser(userId);

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{userId}")
  public ResponseEntity<Void> updateUser(
      @PathVariable UUID userId,
      @Valid @RequestBody UserUpdateRequestDto request) {

    userService.updateUser(userId, request);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> updatePassword(@PathVariable UUID userId,
      @Valid @RequestBody PasswordUpdateRequestDto request) {

    userService.updatePassword(userId, request);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> deleteUser(
      @PathVariable UUID userId) {

    userService.deleteUser(userId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(
      @Valid @RequestBody UserLoginRequestDto request) {

    UserLoginResultDto result = userService.login(request.email(), request.password());

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + result.token());

    Map<String, Object> responseBody = Map.of(
        "id", result.id(),
        "email", result.email(),
        "nickname", result.nickname(),
        "createdAt", result.createdAt()
    );

    return ResponseEntity.ok().headers(headers).body(responseBody);
  }
}
