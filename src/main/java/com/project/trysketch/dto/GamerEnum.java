package com.project.trysketch.dto;

import lombok.RequiredArgsConstructor;

// 1. 기능    : Gamer 공통 사용 열거형 클래스
// 2. 작성자  : 서혁수
@RequiredArgsConstructor
public enum GamerEnum {

    ID("id"),                // PK 즉, 고유번호
    NICK("nickname"),        // 닉네임
    IMG("imgUrl");           // 랜덤 이미지

    private final String key;   // Enum 요소에 특정 값을 매핑하기 위해서 필드값 추가


    public String key() {
        return key;
    }
}
