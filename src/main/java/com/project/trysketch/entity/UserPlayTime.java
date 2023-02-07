package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

// 1. 기능   : 게임 플레이타임 저장
// 2. 작성자 : 김재영
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPlayTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDateTime playStartTime;        // 게임이 시작된 시간

    @Column
    private LocalDateTime playEndTime;          // 게임이 끝난 시간

    @Column
    private Long gameRoomId;                    // 해당 게임 방 id

    @OneToOne
    private GameRoomUser gameRoomUser;          // 플레이타임의 주인

    public void updateUserPlayTime(LocalDateTime playEndTime) {
        this.playEndTime = playEndTime;
    }
}
