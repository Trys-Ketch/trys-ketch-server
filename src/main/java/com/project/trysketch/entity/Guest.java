package com.project.trysketch.entity;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

// 1. 기능   : 비회원 구성요소
// 2. 작성자 : 서혁수
@Getter
@RedisHash(value = "guest", timeToLive = 36000L)
public class Guest implements Serializable {
    // RedisHash : Hash Collection 명시
    // Jpa 의 Entity 에 해당하는 어노테이션 즉, value 는 Key 를 만들 때 사용하는 것으로
    // Key 는 value + @Id 로 형성된다.
    @Id
    private Long id;

    @Indexed                        // key 식별시 사용하는 고유값
    private String guestId;         // redis 에서 key 값(고유번호)

    private String nickname;        // 유저 닉네임

    private String imgUrl;          // 유저 랜덤 이미지

    public Guest(Long id, String guestId, String nickname, String imgUrl) {
        this.id = id;
        this.guestId = guestId;
        this.nickname = nickname;
        this.imgUrl = imgUrl;
    }

}
