package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능    : 게임 방 제목 입력 요소
// 2. 작성자  : 김재영
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomRequestDto {
    private String title;
}
