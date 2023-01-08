package com.project.trysketch.global.rtc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 1. 기능   : WebSocket 통신에 필요한 Message
// 2. 작성자 : 안은솔

@Getter
@Builder
@AllArgsConstructor
public class WebSocketMessage {
    private String from;      // 보내는 유저 UUID
    private String type;      // 메시지 타입
    private String data;      // roomId
    private Object candidate; // 상태
    private Object sdp;       // sdp 정보
}
