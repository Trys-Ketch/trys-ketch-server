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

    @Column(nullable = false)
    private int round;           // 게임 라운드

    @Column(nullable = false)
    private int keywordIndex;    // 키워드 순번

    @Column
    private String keyword;      // 키워드

    @Column
    private String imagePath;    // 그림 이미지

    @Column
    private Long imagePk;        // 그림 이미지 PK

    @Column(nullable = false)
    private String nickname;     // 닉네임

    @Column(nullable = false)
    private Long roomId;         // 방 번호

    @Column(nullable = false)
    private String webSessionId; // webSessionId

    @Column(nullable = false)
    private boolean isSubmitted; // 제거

    @Column(nullable = false)
    private String userImgPath;  // 유저 프로필 이미지
}
