package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

// 1. 기능    : 게임 방 제목 입력 요소
// 2. 작성자  : 김재영
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomRequestDto {
    @Size(min=2, max= 25, message="방제목은 2자이상, 25자이하이어야합니다")
    private String title;
}
