package com.project.trysketch.suggest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 형용사 구성요소
// 2. 작성자 : 서혁수
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class AdjectiveEntity {

    @Id
    @Column(name = "num")
    private int num;

    @Column
    private String adjective;

}