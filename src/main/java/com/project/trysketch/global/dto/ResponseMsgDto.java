package com.project.trysketch.global.dto;

import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능    : Global 메세지 반환값
// 2. 작성자  : 안은솔, 황미경
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

    public ResponseMsgDto(StatusMsgCode statusMsgCode){
        this.statusCode = statusMsgCode.getHttpStatus().value();
        this.message = statusMsgCode.getDetail();
    }
}
