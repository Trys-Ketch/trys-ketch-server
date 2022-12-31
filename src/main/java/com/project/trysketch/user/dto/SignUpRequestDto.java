package com.project.trysketch.user.dto;

import lombok.Getter;

@Getter
public class SignUpRequestDto {

    //    @Pattern(regexp = "[a-z0-9]{4,10}")
    private String email;

    //    @Pattern(regexp = "[a-z0-9A-Z!@#$%^&*]{8,15}")
    private String password;

    private String nickname;
}
