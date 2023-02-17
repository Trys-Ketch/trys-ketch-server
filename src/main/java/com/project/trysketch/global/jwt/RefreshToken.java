package com.project.trysketch.global.jwt;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// 1. 기능   : RefreshToken 구성요소
// 2. 작성자 : 서혁수, 황미경
@Getter
@RedisHash(value = "RefreshToken", timeToLive = 7 * 24 * 60 * 60L)      // 7일
public class RefreshToken {
    @Id
    private String id;          // RefreshToken ID
    private String token;       // RefreshToken 값

    public RefreshToken(String id, String token) {
        this.id = id;
        this.token = token;
    }
}
