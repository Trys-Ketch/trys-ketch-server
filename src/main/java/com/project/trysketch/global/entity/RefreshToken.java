package com.project.trysketch.global.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Getter
@Entity
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 참고 블로그 : https://sanghye.tistory.com/36
    // @NotBlank 는 null 과, "", " " 모두 허용하지 않는다.
    // null 값 비허용에 있어서 가장 강한 비허용의 방법이다.
    @NotBlank
    private String refreshToken;

    @NotBlank
    private String userNickname;

    public RefreshToken(String refreshToken, String userNickname) {
        this.refreshToken = refreshToken;
        this.userNickname = userNickname;
    }

    public RefreshToken updateToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
}
