package com.project.trysketch.user.dto;

import lombok.Getter;

// 1. 기능    : 로그인 시 입력 요소
// 2. 작성자  : 조소영
@Getter
public class SigninRequestDto {
    private String email;       // 유저 이메일
    private String nickname;
    private String password;    // 유저 비밀번호
}
