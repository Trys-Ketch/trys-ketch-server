package com.project.trysketch.global.rtc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

// 1. 기능   : WebRTC관련 ChatMessage domain(사용전)
// 2. 작성자 : 안은솔

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum MessageType {
        ENTER, TALK
    }

    private MessageType type; // 채팅방 입장 or 채팅
    private String roomId;    // 채팅방 id
    private String sender;    // 보내는 사람
    private String message;   // 채팅 내용
}
