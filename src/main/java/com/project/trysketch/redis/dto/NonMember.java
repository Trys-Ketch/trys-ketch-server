package com.project.trysketch.redis.dto;

import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;
import javax.persistence.Id;

// 1. 기능   : 비회원 구성요소
// 2. 작성자 : 서혁수
@Getter
@RedisHash(value = "redisAccess")
public class NonMember {

    @Id
    private Long id;            // redis 에서 key 값(고유번호)

    @Indexed                    // 값으로 검색을 하기 위해서
    private String nickname;    // 유저 닉네임

    @TimeToLive                 // 만료시간 설정(초 단위)
    private Long expired;

    public NonMember(Long id, String nickname, Long expired) {
        this.id = id;
        this.nickname = nickname;
        this.expired = expired;
    }

}
