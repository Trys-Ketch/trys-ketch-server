package com.project.trysketch.redis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestRoomUsersDto {
    private Long id;

    private Long roomNum;       // 유저 고유번호

    private String nickname;    // 유저 닉네임

    private boolean readyStatus;    // 준비 상태

}
