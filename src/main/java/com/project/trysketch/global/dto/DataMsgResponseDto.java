package com.project.trysketch.global.dto;

import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

// 1. 기능   : 메시지와 데이터를 반환하는 dto
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
public class DataMsgResponseDto {
    private int statusCode;
    private String statusMsg;
    private Object data;

    public DataMsgResponseDto(StatusMsgCode statusMsgCode, Object data) {
        this.statusCode = statusMsgCode.getHttpStatus().value();
        this.statusMsg = statusMsgCode.getDetail();
        this.data = data;
    }
}
