package com.part3_team4.deokhoogam.domain.user.dto;

import java.util.UUID;

public record UserDto (
    UUID id,
    String email,
    String name
){

}
