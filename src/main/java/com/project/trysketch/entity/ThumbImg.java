package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

// 1. 기능   : 프로필 이미지 구성요소
// 2. 작성자 : 서혁수
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ThumbImg {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String imgUrl;
}
