package com.project.trysketch.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능    : 소셜 로그인 입력 요소
// 2. 작성자  : 황미경, 김재영, 서혁수
@Getter
@NoArgsConstructor
public class OAuthRequestDto {
    private String id;
    private String email;
    private String nickname;

    public OAuthRequestDto(String id, String nickname, String email) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
    }
}