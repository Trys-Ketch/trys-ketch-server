package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;

// 1. 기능   : 시작된 게임 정보
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class GameInfo {
    
    // 시작된 게임 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 시작된 게임의 방 id
    private Long gameRoomId;
    
    // 게임 방의 현재 라운드
    private int round;
    
    // 게임이 시작될 때 최초로 받을 제시어
    // private HashMap<String, String> defaultKeyword;

//    // 게임이 시작될 때 최초로 받을 제시어
//    private HashMap<String, String> keyword;

    // 그림을 그리는 시간 & 제시어를 적는시간
    private int timeout;

//    private String mode; // 모드 미구현
}
