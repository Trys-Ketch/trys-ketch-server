package com.project.trysketch.suggest;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// 1. 기능   : 형용사 구성요소
// 2. 작성자 : 서혁수
@Data
// @Data 는 @Getter, @Setter, @RequiredArgsConstructor, @ToString, @EqualsAndHashCode
// 을 한꺼번에 설정해주는 어노테이션이다. 아래의 작업에서는 getter 와 setter 모두 필요하다.
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Entity
public class AdjectiveEntity {

    @Id
    @Column(name = "num")
    private int num;

    @Column
    private String adjective;

    //==생성자==//
    public AdjectiveEntity(int num, String adjective) {
        this.num = num;
        this.adjective = adjective;
    }
}