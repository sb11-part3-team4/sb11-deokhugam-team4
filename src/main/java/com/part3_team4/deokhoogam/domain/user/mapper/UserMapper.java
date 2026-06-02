package com.part3_team4.deokhoogam.domain.user.mapper;

import com.part3_team4.deokhoogam.domain.user.dto.UserDto;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public UserDto toDto(User user) {
    if (user == null) {
      return null;
    }

    return new UserDto(
        user.getId(),
        user.getEmail(),
        user.getName()
    );
  }
}
