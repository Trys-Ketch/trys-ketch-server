package com.project.trysketch.redis.entity;

import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;
import javax.persistence.Id;

// 1. 기능   : 비회원 구성요소
// 2. 작성자 : 서혁수
@Getter
@RedisHash(value = "nonMember")
public class NonMember {
    // RedisHash : Hash Collection 명시
    // Jpa 의 Entity 에 해당하는 어노테이션 즉, value 는 Key 를 만들 때 사용하는 것으로
    // Key 는 value + @Id 로 형성된다.

    @Id                         // key 식별시 사용하는 고유값
    private Long id;            // redis 에서 key 값(고유번호)

    @Indexed                    // 값으로(Repo 에서) 검색을 하기 위해서
    private String nickname;    // 유저 닉네임

    @TimeToLive                 // 만료시간 설정(초 단위)
    private Long expired;

    public NonMember(Long id, String nickname, Long expired) {
        this.id = id;
        this.nickname = nickname;
        this.expired = expired;
    }

}
