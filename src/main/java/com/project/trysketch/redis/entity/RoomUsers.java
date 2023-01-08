package com.project.trysketch.redis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import javax.persistence.Id;

// 1. 기능   : 비회원 구성요소
// 2. 작성자 : 서혁수
@Getter
@Builder
@AllArgsConstructor
@RedisHash(value = "roomUsers")
public class RoomUsers {
    @Id                             // key 식별시 사용하는 고유값
    private Long id;                // redis 에서 key 값(고유번호) 여기서는 방번호

    @Indexed
    private Long roomNum;           // 방 고유 번호

    private String nickname;        // 유저 닉네임

    private boolean readyStatus;    // 준비 상태

}
