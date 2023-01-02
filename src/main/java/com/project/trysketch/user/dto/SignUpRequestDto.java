package com.project.trysketch.user.dto;

import lombok.Getter;

// 1. 기능    : 회원가입 시 입력 요소
// 2. 작성자  : 서혁수
@Getter
public class SignUpRequestDto {

    //    @Pattern(regexp = "[a-z0-9]{4,10}")
    private String email;

    //    @Pattern(regexp = "[a-z0-9A-Z!@#$%^&*]{8,15}")
    private String password;

    private String nickname;
}
