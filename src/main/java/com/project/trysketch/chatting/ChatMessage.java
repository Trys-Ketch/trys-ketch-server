package com.project.trysketch.chatting;

import lombok.Getter;
import lombok.Setter;

// 1. 기능    : 채팅 메세지 구성요소
// 2. 작성자  : 황미경, 서혁수, 안은솔
@Getter
@Setter
public class ChatMessage {
    public enum MessageType {
        ENTER, TALK
    }

    private MessageType type;   // 메시지 타입
    private String roomId;      // 방번호
    private String gamerId;     // 메시지 보낸 사람 ID
    private String gamerNick;   // 메시지 보낸 사람 닉네임
    private String message;     // 메시지
}