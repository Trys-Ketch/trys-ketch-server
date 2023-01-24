package com.project.trysketch.entity;

import com.project.trysketch.global.utill.AchievmentCode;
import com.project.trysketch.global.utill.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Entity
public class Achievment extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업적의 이름
    @Column(nullable = false)
    private String name;

    // 그 업적의 주인
    @ManyToOne
    private User user;

    public Achievment(String name, User user) {
        this.name = name;
        this.user = user;
    }


    public Achievment(AchievmentCode achievmentCode, User user) {
        this.name = achievmentCode.getAchievmentName();
        this.user = user;
    }
}
