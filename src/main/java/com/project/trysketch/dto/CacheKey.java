package com.project.trysketch.dto;

// 1. 기능   : 캐시키 만료 시간 설정
// 2. 작성자 : 서혁수
public class CacheKey {

//    private CacheKey() {
//    }

    public static final String USER = "user";               // 캐시키 타입
    public static final int DEFAULT_EXPIRE_SEC = 18000;     // 기본 캐시 만료 시간
    public static final int USER_EXPIRE_SEC = 18000;        // 유저 캐시 만료 시간
}
