package com.project.trysketch.maingame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;

// 1. 기능   : 시작된 게임 정보
// 2. 작성자 : 김재영

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInfo {
    
    // 시작된 게임 id
    private Long id;
    
    // 시작된 게임의 방 id
    private Long gameRoomId;
    
    // 게임 방의 현재 라운드
    private int round;
    
    // 게임이 시작될 때 최초로 받을 제시어
    private HashMap<String, String> defaultKeyword;
    
    // 제시어를 사용하지 않거나 그림을 받은 유저가 다음유저에게 넘겨줄 제시어 
    private HashMap<String, String> customKeyword;
    
    // 그림을 그리는 시간 & 제시어를 적는시간
    private int timeout;

//    private String mode; // 모드 미구현
}
