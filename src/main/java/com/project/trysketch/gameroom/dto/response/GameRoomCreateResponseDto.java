package com.project.trysketch.gameroom.dto.response;

import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

// 1. 기능   : 게임방 create시에만 사용할 response Dto
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
public class GameRoomCreateResponseDto<HashMap> {
    private  HttpStatus httpStatus;
    private String statusMsg;
    private HashMap data;

    public GameRoomCreateResponseDto(StatusMsgCode statusMsgCode, HashMap data) {
        this.httpStatus = statusMsgCode.getHttpStatus();
        this.statusMsg = statusMsgCode.getDetail();
        this.data = data;
    }
}
