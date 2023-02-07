package com.project.trysketch.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 형용사 구성요소
// 2. 작성자 : 서혁수
@Entity
@Getter
@NoArgsConstructor
public class Adjective {

    @Id
    private int num;                // 형용사 번호

    @Column
    private String adjective;       // 형용사

}