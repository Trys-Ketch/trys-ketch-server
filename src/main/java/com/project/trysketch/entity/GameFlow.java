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

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String webSessionId;

    @Column(nullable = false)
    private boolean isSubmitted;

    public void update(boolean isSubmitted) {
        this.isSubmitted = isSubmitted;
    }

    public void update(boolean isSubmitted, String keyword) {
        this.isSubmitted = isSubmitted;
        this.keyword = keyword;
    }

    public void update(String imagePath, boolean isSubmitted) {
        this.isSubmitted = isSubmitted;
        this.imagePath = imagePath;
    }
}
