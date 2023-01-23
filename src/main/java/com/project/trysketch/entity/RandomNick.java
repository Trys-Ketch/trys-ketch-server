package com.project.trysketch.entity;

import lombok.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 랜덤 닉네임 구성요소
// 2. 작성자 : 서혁수
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class RandomNick {

    @Id
    @Column(name = "num")
    private int num;

    @Column
    private String nickname;

}