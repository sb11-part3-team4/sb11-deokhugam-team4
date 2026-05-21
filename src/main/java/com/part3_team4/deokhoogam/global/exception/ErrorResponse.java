package com.part3_team4.deokhoogam.global.exception;

import java.time.Instant;
import lombok.Builder;


@Builder
public record ErrorResponse (


    Instant timestamp,
    String code,
    String message,
    Object details,
    String exceptionType,
    int status

){
}
