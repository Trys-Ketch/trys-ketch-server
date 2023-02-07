package com.project.trysketch.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 랜덤 닉네임 구성요소
// 2. 작성자 : 서혁수
@Entity
@Getter
@NoArgsConstructor
public class RandomNick {

    @Id
    private int num;                // 랜덤닉네임 번호

    @Column
    private String nickname;        // 랜덤 닉네임

}