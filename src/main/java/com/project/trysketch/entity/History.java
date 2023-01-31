package com.project.trysketch.entity;

import com.project.trysketch.global.utill.Timestamped;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.*;

// 1. 기능   : 유저 활동 내역
// 2. 작성자 : 김재영
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class History extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 플레이 타임 ( 게임 시작 - 게임 끝 )
    @Column
    private Long playtime;

    // 플레이 횟수
    @Column
    private Long trials;

    // 로그인 횟수
    @Column
    private Long visits;

    // 1:1 연관관계 - User
    @OneToOne
    private User user;

    public History updatePlaytime(Long playtime) {
        this.playtime += playtime;
        return this;
    }

    public History updateTrials(Long trials) {
        this.trials += trials;
        return this;
    }

    public History updateVisits(Long visits) {
        this.visits += visits;
        return this;
    }

    public void updateUser(User user) {
        this.user = user;
    }
}
