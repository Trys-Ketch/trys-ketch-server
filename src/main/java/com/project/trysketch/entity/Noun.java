package com.project.trysketch.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 명사 구성요소
// 2. 작성자 : 서혁수
@Entity
@Getter
@NoArgsConstructor
public class Noun {

    @Id
    private int num;            // 명사 번호

    @Column
    private String noun;        // 명사

}