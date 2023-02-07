package com.project.trysketch.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 명사 구성요소
// 2. 작성자 : 서혁수
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Entity
public class Noun {

    @Id
    @Column(name = "num")
    private int num;

    @Column
    private String noun;

    //==생성자==//
    public Noun(int num, String noun) {
        this.num = num;
        this.noun = noun;
    }
}