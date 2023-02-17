package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

// 1. 기능   : 제출 인원
// 2. 작성자 : 안은솔
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameFlowCount {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private int gameFlowCount;   // 제출 인원

    @Column(nullable = false)
    private int round;           // 라운드

    @Column(nullable = false)
    private Long roomId;         // 방 번호

    public GameFlowCount update(int gameFlowListSize) {
        this.gameFlowCount += gameFlowListSize;
        return this;
    }
}
