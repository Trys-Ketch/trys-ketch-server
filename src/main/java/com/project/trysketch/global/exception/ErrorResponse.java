package com.project.trysketch.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
public class ErrorResponse {

    private final int statusCode;
    private final String message;

    public static ResponseEntity<ErrorResponse> toResponseEntity(StatusMsgCode statusMsgCode) {
        return ResponseEntity
                .status(statusMsgCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .statusCode(statusMsgCode.getHttpStatus().value())
                        .message(statusMsgCode.getDetail())
                        .build());
    }

}