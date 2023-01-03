package com.project.trysketch.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능    : OAuth2.0 카카오 입력 요소
// 2. 작성자  : 황미경

@Getter
@NoArgsConstructor
public class KakaoUserRequstDto {
    private Long id;
    private String email;
    private String nickname;

    public KakaoUserRequstDto(Long id, String nickname, String email) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
    }
}