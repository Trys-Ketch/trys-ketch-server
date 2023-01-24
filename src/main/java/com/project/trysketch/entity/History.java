package com.project.trysketch.entity;

import com.project.trysketch.global.utill.Timestamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.*;

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
    @OneToOne(mappedBy = "history")
    private User user;

    public void updatePlaytime(Long playtime) {
        this.playtime = playtime;
    }

    public void updateTrials(Long trials) {
        this.trials = trials;
    }

    public void updateVisits(Long visits) {
        this.visits = visits;
    }
}
