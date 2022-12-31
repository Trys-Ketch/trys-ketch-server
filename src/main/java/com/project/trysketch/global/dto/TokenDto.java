package com.project.trysketch.global.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능   : 토큰 동시 발급에 사용되는 DTO
// 2. 작성자 : 서혁수
@Getter
@NoArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;

    public TokenDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
