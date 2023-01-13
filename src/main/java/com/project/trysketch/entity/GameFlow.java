package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.*;

// 1. 기능    : 게임 진행중 세부 정보
// 2. 작성자  : 김재영, 황미경
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameFlow {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private int round;

    @Column
    private int keywordIndex;

    @Column
    private String keyword;

    @Column
    private String imagePath;

    @Column
    private String nickname;

    @Column(nullable = false)
    private Long roomId;
}
