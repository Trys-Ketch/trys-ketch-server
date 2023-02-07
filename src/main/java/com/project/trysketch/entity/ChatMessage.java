package com.project.trysketch.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 1. 기능    : 채팅 메세지 구성요소
// 2. 작성자  : 황미경, 서혁수, 안은솔
@Getter
@Setter
public class ChatMessage {
    public enum MessageType { LEAVE }

    private MessageType type;   // 메시지 타입
    private String roomId;      // 방번호
    private String userId;      // 메시지 보낸사람의 ID
    private String nickname;    // 메시지 보낸사람의 닉네임
    private String content;     // 메시지 내용


    @Builder
    public ChatMessage(MessageType type, String roomId, String userId, String nickname, String content) {
        this.type = type;
        this.roomId = roomId;
        this.userId = userId;
        this.nickname = nickname;
        this.content = content;
    }
}