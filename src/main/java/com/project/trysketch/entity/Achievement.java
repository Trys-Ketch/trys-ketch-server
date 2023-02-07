package com.project.trysketch.entity;

import com.project.trysketch.global.utill.AchievementCode;
import com.project.trysketch.global.utill.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;

// 1. 기능   : 업적
// 2. 작성자 : 김재영
@NoArgsConstructor
@Getter
@Entity
public class Achievement extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;            // 업적의 이름

    @ManyToOne
    private User user;              // 업적의 주인

    public Achievement(AchievementCode achievementCode, User user) {
        this.name = achievementCode.getAchievementName();
        this.user = user;
    }
}
