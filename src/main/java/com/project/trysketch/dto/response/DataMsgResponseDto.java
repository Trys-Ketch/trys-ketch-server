package com.project.trysketch.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능   : 메시지와 데이터를 반환하는 dto
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataMsgResponseDto<T> {
    private int statusCode;
    private String message;
    private T data;

    public DataMsgResponseDto(StatusMsgCode statusMsgCode, T data) {
        this.statusCode = statusMsgCode.getHttpStatus().value();
        this.message = statusMsgCode.getDetail();
        this.data = data;
    }

    public DataMsgResponseDto(StatusMsgCode statusMsgCode) {
        this.statusCode = statusMsgCode.getHttpStatus().value();
        this.message = statusMsgCode.getDetail();
    }

    public DataMsgResponseDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}