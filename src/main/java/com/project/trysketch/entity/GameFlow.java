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
    private int round;

    @Column(nullable = false)
    private int keywordIndex;

    @Column
    private String keyword;

    @Column
    private String imagePath;

    @Column
    private Long imagePk; // 수정 추가 김재영 01.29

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String webSessionId;

    @Column(nullable = false)
    private boolean isSubmitted;

    @Column(nullable = false)
    private String userImgPath;

    public GameFlow update(boolean isSubmitted) {
//        this.id = id;
        this.isSubmitted = isSubmitted;
        return this;
    }

    public GameFlow update(boolean isSubmitted, String keyword) {
//        this.id = id;
        this.isSubmitted = isSubmitted;
        this.keyword = keyword;
        return this;
    }

    public GameFlow update(String imagePath, boolean isSubmitted) {
//        this.id = id;
        this.isSubmitted = isSubmitted;
        this.imagePath = imagePath;
        return this;
    }
}
