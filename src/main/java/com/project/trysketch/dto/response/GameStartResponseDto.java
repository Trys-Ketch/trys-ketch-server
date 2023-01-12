package com.project.trysketch.dto.response;

import lombok.*;

import java.util.HashMap;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class GameStartResponseDto {

    private Long gameRoomId;
    private Integer roundTimeout;

    // 게임이 시작될 때 최초로 받을 제시어
    private HashMap<String, String> keyword;
}
