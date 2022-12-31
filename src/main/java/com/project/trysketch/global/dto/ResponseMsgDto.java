package com.project.trysketch.global.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

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
