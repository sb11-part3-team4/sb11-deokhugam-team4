package com.part3_team4.deokhoogam.domain.user.entity;

import java.util.UUID;

public record UserDeletedEvent(
    UUID userId,
    boolean isHardDelete //논리 삭제(false)와 물리 삭제(true)를 구분하는 플래그
) {
}
