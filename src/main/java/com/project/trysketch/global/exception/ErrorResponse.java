package com.project.trysketch.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

// 1. 기능    : 통합 에러 메세지 생성자
// 2. 작성자  : 안은솔
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