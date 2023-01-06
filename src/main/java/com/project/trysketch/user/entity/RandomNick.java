package com.project.trysketch.user.entity;

import lombok.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 랜덤 닉네임 구성요소
// 2. 작성자 : 서혁수
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Entity
public class RandomNick {

    @Id
    @Column(name = "num")
    private int num;

    @Column
    private String nickname;

    //==생성자==//
    public RandomNick(int num, String nickname) {
        this.num = num;
        this.nickname = nickname;
    }
}