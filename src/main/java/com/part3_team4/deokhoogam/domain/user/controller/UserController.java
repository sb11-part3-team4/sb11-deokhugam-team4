package com.part3_team4.deokhoogam.domain.user.controller;

import com.part3_team4.deokhoogam.domain.user.dto.UserCreateRequestDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.response.UserResponse;
import com.part3_team4.deokhoogam.domain.user.dto.UserUpdateRequestDto;
import com.part3_team4.deokhoogam.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserDto> signup(
      @Valid @RequestPart("request") UserCreateRequestDto request,
      @RequestPart(value = "profileImage", required = false)MultipartFile profileImage) {

    UserDto responseDto = userService.createUser(request, profileImage);

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
      @Valid @RequestPart("request") UserUpdateRequestDto request,
      @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

    userService.updateUser(userId, request, profileImage);

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> deleteUser(
      @PathVariable UUID userId) {

    userService.deleteUser(userId);

    return ResponseEntity.noContent().build();
  }
}
