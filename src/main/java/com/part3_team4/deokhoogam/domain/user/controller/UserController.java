package com.part3_team4.deokhoogam.domain.user.controller;

import com.part3_team4.deokhoogam.domain.user.dto.PasswordUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserLoginResultDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.PowerUserRankingResponseDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingService;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final PowerUserRankingService powerUserRankingService;

  public UserController(UserService userService, PowerUserRankingService powerUserRankingService) {
    this.userService = userService;
    this.powerUserRankingService = powerUserRankingService;
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

  @DeleteMapping("/{userId}/hard")
  public ResponseEntity<Void> hardDeleteUser(@PathVariable UUID userId) {

    userService.hardDeleteUser(userId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  public ResponseEntity<UserLoginResultDto> login(
      @Valid @RequestBody UserLoginRequestDto request) {

    UserLoginResultDto result = userService.login(request.email(), request.password());

    return ResponseEntity.ok(result);
  }

  @GetMapping("/power")
  public ResponseEntity<Map<String, Object>> getPowerUsers(
      @RequestParam(defaultValue = "DAILY") PowerUserPeriod period) {

    List<PowerUserRankingResponseDto> rankings = powerUserRankingService.getRankingWithNickname(period);

    Map<String, Object> response = Map.of("content", rankings);

    return ResponseEntity.ok(response);
  }
}
