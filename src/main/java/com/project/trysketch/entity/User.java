package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;

// 1. 기능   : 유저 구성요소
// 2. 작성자 : 서혁수, 황미경
@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;           // 유저의 이메일

    @Column(nullable = false)
    private String nickname;        // 유저의 닉네임

    @Column(nullable = false)
    private String password;        // 유저의 비밀번호

    @Column
    private Long kakaoId;           // 유저의 카카오ID

    @Column
    private String naverId;        // 유저의 네이버ID

    @Column
    private Long googleId;         // 유저의 구글ID

    @Column(nullable = false)
    private String imgUrl;         // 유저의 프로필사진 URL

    @OneToOne(mappedBy = "user")
    private History history;       // 유저의 업적

    public User kakaoIdUpdate(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }
    public User naverIdUpdate(String naverId) {
        this.naverId = naverId;
        return this;
    }

    public User googleIdUpdate(Long googleId) {
        this.googleId = googleId;
        return this;
    }

    public User update(String nickname, String imgUrl) {
        this.nickname = nickname;
        this.imgUrl = imgUrl;
        return this;
    }
}
