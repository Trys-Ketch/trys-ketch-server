package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.validation.constraints.Pattern;

// 1. 기능    : 게임 방 제목 입력 요소
// 2. 작성자  : 김재영
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomRequestDto {
    @Pattern(regexp = "^[a-zA-Z0-9가-힣`~!@#$%^&*()_=+|{};:,.<>/?]{2,25}$", message = "방제목은 2자이상, 25자이하이어야하며 한글, 대소문자, 숫자, 특수문자만 가능합니다.")
    private String title;
}
