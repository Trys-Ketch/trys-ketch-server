package com.project.trysketch.chatting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {

    // 메시지 타입 : 입장, 채팅
    public enum MessageType {
        ENTER, TALK
    }

    private MessageType type;   // 메시지 타입
    private String roomId;      // 방번호
    private String gamerId;     // 메시지 보낸 사람 ID
    private String gamerNick;   // 메시지 보낸 사람 닉네임
    private String message;     // 메시지
}