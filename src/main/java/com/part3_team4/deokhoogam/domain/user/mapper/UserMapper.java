package com.part3_team4.deokhoogam.domain.user.mapper;

import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.dto.UserProfileImageDto;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.entity.UserProfileImage;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public UserDto toDto(User user) {
    if (user == null) {
      return null;
    }

    UserProfileImageDto profileDto = null;
    if (user.getProfileImage() != null) {
      profileDto = toProfileImageDto(user.getProfileImage());
    }

    return new UserDto(
        user.getId(),
        user.getEmail(),
        user.getName(),
        profileDto
    );
  }

  public UserProfileImageDto toProfileImageDto(UserProfileImage profileImage) {
    if (profileImage == null) {
      return null;
    }

    return new UserProfileImageDto(
        profileImage.getId(),
        profileImage.getFileName(),
        profileImage.getSize(),
        profileImage.getContentType()
    );
  }
}
