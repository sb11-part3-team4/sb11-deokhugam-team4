package com.part3_team4.deokhoogam.domain.book.entity;

import java.util.UUID;

public record BookDeletedEvent(
    UUID bookId,
    boolean isHardDelete //논리 삭제(false), 물리 삭제(true)
) {

}
