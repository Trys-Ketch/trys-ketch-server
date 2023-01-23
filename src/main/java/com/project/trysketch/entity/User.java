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
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column
    private Long kakaoId;

    @Column
    private Long googleId;

    @Column(nullable = false)
    private String imgUrl;

//    public User(String email, String nickname, String password) {
//        this.email = email;
//        this.nickname = nickname;
//        this.password = password;
//    }
//
    public User kakaoIdUpdate(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }

    public User googleIdUpdate(Long googleId) {
        this.googleId = googleId;
        return this;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    //    @Builder
//    public User(String email, String password, Long kakaoId, String kakaoNickname) {
//        this.email = email;
//        this.password = password;
//        this.kakaoId = kakaoId;
//        this.nickname = kakaoNickname;
//    }

    public User update(String nickname, String imgUrl) {
        this.nickname = nickname;
        this.imgUrl = imgUrl;

        return this;
    }
}
