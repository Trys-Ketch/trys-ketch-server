package com.project.trysketch.global.rtc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능   : 클라이언트와 주고 받을 Message 객체
// 2. 작성자 : 안은솔
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String sender;    // 보내는 유저 UUID
    private String type;      // 메시지 타입
    private String receiver;  // 받는 사람
    private Long room;        // roomId
    private Object candidate; // 상태
    private Object sdp;       // sdp 정보
    private Object allUsers;  // 해당 방에 본인을 제외한 전체 유저
    private String token;     // 로그인한 유저의 토큰

    private boolean status;   // 유저의 레디 상태
    private boolean host;     // 접속한 유저의 호스트 여부
    private String hostId;    // 해당 방 host 의 sessionId
    private Object attendee;  // 해당 방에 본인을 포함한 전체 유저
    private String kickId;    // 강퇴가 되는 유저
}
