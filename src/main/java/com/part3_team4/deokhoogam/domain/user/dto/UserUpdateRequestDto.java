package com.part3_team4.deokhoogam.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDto(

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String newEmail,

    @Size(min= 2,max= 20,message= "사용자명은 2~20자여야 합니다.")
    String newName
) {

}
