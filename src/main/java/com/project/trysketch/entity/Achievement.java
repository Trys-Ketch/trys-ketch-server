package com.project.trysketch.entity;

import com.project.trysketch.global.utill.AchievementCode;
import com.project.trysketch.global.utill.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

// 1. 기능   : 유저 활동 내역
// 2. 작성자 : 김재영
@NoArgsConstructor
@Getter
@Entity
public class Achievement extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업적의 이름
    @Column(nullable = false)
    private String name;

    // 그 업적의 주인
    @ManyToOne
    private User user;


    public Achievement(AchievementCode achievementCode, User user) {
        this.name = achievementCode.getAchievmentName();
        this.user = user;
    }
}
