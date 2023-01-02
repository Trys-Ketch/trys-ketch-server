package com.project.trysketch.global.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능    : Global 메세지 반환값
// 2. 작성자  : 안은솔
@Getter
@NoArgsConstructor
public class ResponseMsgDto {

    private int statusCode;
    private String message;

    public ResponseMsgDto(int statusCode) {
        this.statusCode = statusCode;
    }
    public ResponseMsgDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
