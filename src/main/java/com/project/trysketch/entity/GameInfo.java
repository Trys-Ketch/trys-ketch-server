package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

// 1. 기능   : 시작된 게임 정보
// 2. 작성자 : 김재영, 황미경
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

    // 그림을 그리는 시간 & 제시어를 적는시간
    private int roundTimeout;

    @OneToOne
    private GameFlow keyword;

//    private String mode; // 모드 미구현
}
