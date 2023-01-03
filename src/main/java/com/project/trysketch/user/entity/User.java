package com.project.trysketch.user.entity;

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

    private Long kakaoId;

    public User(String email, String nickname, String password) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }

    public User kakaoIdUpdate(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }

    @Builder
    public User(String email, String password, Long kakaoId, String kakaoNickname) {
        this.email = email;
        this.password = password;
        this.kakaoId = kakaoId;
        this.nickname = kakaoNickname;
    }
}
